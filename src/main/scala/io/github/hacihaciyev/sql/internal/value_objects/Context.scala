package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.build_errors.SchemaVerificationException
import io.github.hacihaciyev.config.Conf
import io.github.hacihaciyev.sql.value_objects.{Projection, TableRef}
import io.github.hacihaciyev.sql.expressions.{ColumnRef, Expr, ValueExpr}
import io.github.hacihaciyev.sql.expressions.ColumnRef.*
import io.github.hacihaciyev.sql.internal.schema.{Column, SchemaResolver, Table}
import io.github.hacihaciyev.sql.internal.value_objects.{Ref, ExprTraversal}
import io.github.hacihaciyev.types.SQLType
import io.github.hacihaciyev.util.{Err, Ok}
import io.github.hacihaciyev.types.internal.{TypeInfo, TypeInfoOk, TypeRegistry}

import scala.jdk.CollectionConverters.*
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

sealed trait Context {
    def trefs: List[TableRef]
    def refs: List[Ref]

    require(trefs != null)
    require(refs != null)
    require(!trefs.contains(null))
    require(!refs.contains(null))

    validate()

    protected def validate(): Unit
}

object Context {

    case class Select(trefs: List[TableRef],
                      refs: List[Ref],
                      where: Option[Expr],
                      groupBy: List[Expr],
                      having: Option[Expr],
                      orderBy: List[Expr]) extends Context {

        require(where != null)
        require(groupBy != null)
        require(having != null)
        require(orderBy != null)

        protected def validate(): Unit = {
            validateCommon(trefs, refs)
            val tables = loadTables(trefs)
            val errs   = mutable.ListBuffer[String]()

            validateProjection(trefs, refs, tables, errs)

            val whereCrefs  = where.toList.flatMap(ExprTraversal.collectCrefs)
            val groupCrefs  = groupBy.flatMap(ExprTraversal.collectCrefs)
            val havingCrefs = having.toList.flatMap(ExprTraversal.collectCrefs)
            val orderCrefs  = orderBy.flatMap(ExprTraversal.collectCrefs)

            val extraCrefs = whereCrefs ++ groupCrefs ++ havingCrefs ++ orderCrefs

            for (cref <- extraCrefs) validateCref(cref, tables, errs)
            throwIfErrors(errs)
        }
    }

    case class Insert(trefs: List[TableRef],
                      refs: List[Ref],
                      returning: Option[List[Ref]]) extends Context {

        require(returning != null)

        protected def validate(): Unit = {
            validateCommon(trefs, refs)
            val tables = loadTables(trefs)
            val errs   = mutable.ListBuffer[String]()

            validateNamedOnly(refs, errs)
            validateProjection(trefs, refs, tables, errs)
            returning.foreach(r => validateProjection(trefs, r, tables, errs))
            throwIfErrors(errs)
        }
    }

    case class Update(trefs: List[TableRef],
                      refs: List[Ref],
                      where: Option[Expr],
                      returning: Option[List[Ref]]) extends Context {

        require(returning != null)

        protected def validate(): Unit = {
            validateCommon(trefs, refs)
            val tables = loadTables(trefs)
            val errs   = mutable.ListBuffer[String]()

            validateNamedOnly(refs, errs)
            validateProjection(trefs, refs, tables, errs)


            val whereCrefs = where.toList.flatMap(ExprTraversal.collectCrefs)
            for (cref <- whereCrefs) validateCref(cref, tables, errs)

            returning.foreach(r => validateProjection(trefs, r, tables, errs))
            throwIfErrors(errs)
        }
    }

    case class Delete(trefs: List[TableRef],
                      refs: List[Ref],
                      where: Option[Expr],
                      returning: Option[List[Ref]]) extends Context {

        require(returning != null)

        protected def validate(): Unit = {
            validateCommon(trefs, refs)
            val tables = loadTables(trefs)
            val errs   = mutable.ListBuffer[String]()

            val whereCrefs = where.toList.flatMap(ExprTraversal.collectCrefs)
            for (cref <- whereCrefs) validateCref(cref, tables, errs)

            returning.foreach(r => validateProjection(trefs, r, tables, errs))
            throwIfErrors(errs)
        }
    }

    private def validateCommon(trefs: List[TableRef], refs: List[Ref]): Unit = {
        if (trefs.isEmpty) {
            assert(refs.isEmpty, "Refs without TableRefs")
            return
        }

        val indexes = refs.collect { case r: Ref.Indexed => r.position }
        if (indexes.toSet.size != indexes.size)
            throw SchemaVerificationException("Duplicate positional indexes found in context.")

        val names = effectiveTableNames(trefs)
        if (names.toSet.size != names.size)
            throw SchemaVerificationException("Duplicate table effective names found in context.")
    }

    private def effectiveTableNames(trefs: List[TableRef]): List[String] = trefs.map {
        case ta: TableRef.Aliased => ta.alias()
        case base                 => base.name()
    }

    private def validateProjection(trefs: List[TableRef],
                                   refs: List[Ref],
                                   tables: Map[TableRef, Table],
                                   errs: ListBuffer[String]): Unit = {

        val trefByName = effectiveTableNames(trefs).zip(trefs).toMap
        val crefs      = mutable.ListBuffer[ColumnRef]()

        val names = refs.map(_.value).flatMap {
            case base: Projection.Base => base.expr match {
                case ac: ColumnRef.AliasedColumn =>
                    crefs.addOne(ac match {
                        case a: Alias          => a
                        case va: VariableAlias => va
                    })
                    List(ac.alias())
                case col: ColumnRef =>
                    crefs.addOne(col)
                    List(col.name())
                case ve: ValueExpr =>
                    crefs.addAll(ExprTraversal.collectCrefs(ve))
                    List()
            }

            case aliased: Projection.Aliased =>
                crefs.addAll(ExprTraversal.collectCrefs(aliased.expr()))
                List(aliased.alias())

            case qw: Projection.QualifiedWildcard =>
                trefByName.get(qw.qualifier()) match {
                    case Some(tref) => loadCrefs(tref, crefs)
                    case None => throw SchemaVerificationException(s"Wildcard qualifier '${qw.qualifier()}' refers to non-existent table.")
                }

            case _: Projection.Wildcard =>
                trefs.flatMap(tref => loadCrefs(tref, crefs))
        }

        if (names.toSet.size != names.size)
            throw SchemaVerificationException("Duplicate column names or aliases found in context.")

        for (cref <- crefs.toList) validateCref(cref, tables, errs)
    }

    private def validateNamedOnly(refs: List[Ref], errs: ListBuffer[String]): Unit =
        refs.foreach {
            case _: Ref.Named   =>
            case _: Ref.Indexed => errs.addOne("DML context does not allow positional refs")
        }

    private def validateCref(cref: ColumnRef,
                             tables: Map[TableRef, Table],
                             errs: ListBuffer[String]): Unit = {

        val matches = tables.filter { case (tref, table) => table.column(cref, tref).isPresent }

        matches.size match {
            case 0 => errs.addOne(s"Column '$cref' not found in any table")
            case 1 =>
                val (tref, table) = matches.head
                val column        = table.column(cref, tref).get
                (cref.typeClass, column) match {
                    case (tpe: Type.Some, col: Column.Known) => validateTypes(cref, tpe.value(), col.sqlType(), errs)
                    case _                                   =>
                }
            case _ => errs.addOne(s"Ambiguous column '$cref' found in multiple tables: ${matches.keys.map(_.name()).mkString(", ")}")
        }
    }

    private def validateTypes(cref: ColumnRef,
                              javaType: Class[?],
                              sqlType: SQLType,
                              errs: mutable.ListBuffer[String]): Unit =

        TypeRegistry.info(javaType) match {
            case typeInfo: TypeInfoOk =>
                if (!typeInfo.sqlTypes().contains(sqlType)) {
                    errs.addOne(typeMismatchError(cref, javaType, sqlType, typeInfo.sqlTypes().asScala.toSet))
                }
            case _: TypeInfo.None => errs.addOne(unsupportedType(cref, javaType))
        }

    private def loadTables(trefs: List[TableRef]): Map[TableRef, Table] =
        trefs.flatMap { tref =>
            SchemaResolver.load(tref, ds()) match {
                case ok: Ok[Table, SchemaVerificationException]   => Some(tref -> ok.value())
                case err: Err[Table, SchemaVerificationException] => throw SchemaVerificationException(s"Table '$tref' not found", err.err())
            }
        }.toMap

    private def loadCrefs(tref: TableRef, crefs: mutable.ListBuffer[ColumnRef]): List[String] = {
        val cols = SchemaResolver.load(tref, ds()) match {
            case ok: Ok[Table, SchemaVerificationException]   => List.from(ok.value().columns().map(_.name()))
            case err: Err[Table, SchemaVerificationException] => throw err.err()
        }
        crefs.addAll(cols.map(s => ColumnRef.Base(s)))
        cols
    }

    private def throwIfErrors(errs: ListBuffer[String]): Unit =
        if (errs.nonEmpty) throw new SchemaVerificationException(s"Schema validation failed:\n  - ${errs.mkString("\n  - ")}")

    private def ds() = Conf.INSTANCE.dataSource().orElseThrow(() => SchemaVerificationException("Cannot obtain a datasource"))

    private def typeMismatchError(cref: ColumnRef, javaType: Class[?], sqlType: SQLType, compatible: Set[SQLType]): String =
        s"Type mismatch for column '$cref': Java type '${javaType.getSimpleName}' is not compatible with SQL type '$sqlType'. Expected one of: $compatible"

    private def unsupportedType(cref: ColumnRef, javaType: Class[?]): String = s"Unsupported Java type '${javaType.getName}' for column '$cref'"
}