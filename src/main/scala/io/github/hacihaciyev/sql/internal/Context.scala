package io.github.hacihaciyev.sql.internal

import io.github.hacihaciyev.build_errors.SchemaVerificationException
import io.github.hacihaciyev.config.Conf
import io.github.hacihaciyev.sql.value_objects.{Projection, TableRef}
import io.github.hacihaciyev.sql.expressions.{ColumnRef, Expr, ValueExpr}
import io.github.hacihaciyev.sql.expressions.ColumnRef.*
import io.github.hacihaciyev.sql.internal.schema.{Column, SchemaResolver, Table}
import io.github.hacihaciyev.sql.internal.value_objects.{Ref, TableSource, OnConflict}
import io.github.hacihaciyev.types.SQLType
import io.github.hacihaciyev.util.{Err, Ok}
import io.github.hacihaciyev.types.internal.{TypeInfo, TypeInfoOk, TypeRegistry}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

sealed trait Context {
    def sources: List[TableSource]
    def refs: List[Ref]
    def outer: Option[Context]

    require(sources != null)
    require(refs != null)
    require(outer != null)
    require(!sources.contains(null))
    require(!refs.contains(null))

    validate()

    protected def validate(): Unit
}

trait DQL

trait DML

object Context {

    case class Select(
                         sources: List[TableSource],
                         refs: List[Ref],
                         where: Option[Expr],
                         groupBy: List[Expr],
                         having: Option[Expr],
                         orderBy: List[Expr],
                         outer: Option[Context] = None
                     ) extends Context, DQL {

        require(groupBy != null)
        require(orderBy != null)

        protected def validate(): Unit = {
            validateCommon(sources, refs)
            val (physical, virtual) = resolve(sources)
            val errs = mutable.ListBuffer[String]()

            validateProjection(sources, refs, physical, virtual, errs)

            val whereCrefs  = where.toList.flatMap(ExprTraversal.collectCrefs)
            val groupCrefs  = groupBy.flatMap(ExprTraversal.collectCrefs)
            val havingCrefs = having.toList.flatMap(ExprTraversal.collectCrefs)
            val orderCrefs  = orderBy.flatMap(ExprTraversal.collectCrefs)

            val extraCrefs  = whereCrefs ++ groupCrefs ++ havingCrefs ++ orderCrefs

            for (cref <- extraCrefs) validateCref(cref, physical, virtual, outer, errs)
            throwIfErrors(errs)
        }
    }

    case class Insert(
                         sources: List[TableSource],
                         refs: List[Ref],
                         conflict: Option[OnConflict],
                         returning: List[Ref],
                         outer: Option[Context] = None
                     ) extends Context, DML {

        require(conflict != null)
        require(returning != null)

        protected def validate(): Unit = {
            validateCommon(sources, refs)
            val (physical, virtual) = resolve(sources)
            val errs = mutable.ListBuffer[String]()

            validateNamedOnly(refs, errs)
            validateProjection(sources, refs, physical, virtual, errs)
            
            conflict.foreach(c => validateConflict(c, physical, virtual, errs))
            
            if returning.nonEmpty then validateProjection(sources, returning, physical, virtual, errs)
            throwIfErrors(errs)
        }
    }

    case class Update(
                         sources: List[TableSource],
                         refs: List[Ref],
                         where: Option[Expr],
                         returning: List[Ref],
                         outer: Option[Context] = None
                     ) extends Context, DML {

        require(returning != null)

        protected def validate(): Unit = {
            validateCommon(sources, refs)
            val (physical, virtual) = resolve(sources)
            val errs = mutable.ListBuffer[String]()

            validateNamedOnly(refs, errs)
            validateProjection(sources, refs, physical, virtual, errs)

            val whereCrefs = where.toList.flatMap(ExprTraversal.collectCrefs)
            for (cref <- whereCrefs) validateCref(cref, physical, virtual, outer, errs)

            if returning.nonEmpty then validateProjection(sources, returning, physical, virtual, errs)
            throwIfErrors(errs)
        }
    }

    case class Delete(
                         sources: List[TableSource],
                         where: Option[Expr],
                         returning: List[Ref],
                         outer: Option[Context] = None
                     ) extends Context, DML {

        require(returning != null)
        
        override def refs: List[Ref] = List.empty

        protected def validate(): Unit = {
            validateCommon(sources, refs)
            val (physical, virtual) = resolve(sources)
            val errs = mutable.ListBuffer[String]()

            val whereCrefs = where.toList.flatMap(ExprTraversal.collectCrefs)
            for (cref <- whereCrefs) validateCref(cref, physical, virtual, outer, errs)

            if returning.nonEmpty then validateProjection(sources, returning, physical, virtual, errs)
            throwIfErrors(errs)
        }
    }

    private def resolve(sources: List[TableSource]): (Map[TableRef, Table], Map[String, List[String]]) = {
        val physical = mutable.Map[TableRef, Table]()
        val virtual  = mutable.Map[String, List[String]]()

        sources.foreach {
            case source: TableSource.Physical =>
                SchemaResolver.load(source.tref, ds()) match {
                    case ok: Ok[Table, SchemaVerificationException]   => physical(source.tref) = ok.value()
                    case err: Err[Table, SchemaVerificationException] => throw SchemaVerificationException(tableNotFound(source), err.err())
                }

            case source: TableSource.Virtual =>
                val cols = extractVirtualCols(source.ctx)
                virtual(source.effectiveName) = cols
        }

        (physical.toMap, virtual.toMap)
    }

    private def extractVirtualCols(ctx: Context): List[String] =
        ctx.refs.map(_.value).flatMap {
            case base: Projection.Base => base.expr match {
                case ac: ColumnRef.AliasedColumn => List(ac.alias())
                case col: ColumnRef => List(col.name())
                case _: ValueExpr => List()
            }
            case aliased: Projection.Aliased => List(aliased.alias())
            case _: Projection.Wildcard => extractAllCols(ctx.sources)
            case qw: Projection.QualifiedWildcard => extractQualifiedCols(qw.qualifier(), ctx.sources)
        }

    private def extractAllCols(sources: List[TableSource]): List[String] =
        sources.flatMap {
            case src: TableSource.Physical => loadPhysicalColNames(src.tref)
            case src: TableSource.Virtual => extractVirtualCols(src.ctx)
        }

    private def extractQualifiedCols(qualifier: String, sources: List[TableSource]): List[String] =
        sources.find(_.effectiveName.equalsIgnoreCase(qualifier)) match {
            case Some(src: TableSource.Physical) => loadPhysicalColNames(src.tref)
            case Some(src: TableSource.Virtual) => extractVirtualCols(src.ctx)
            case None => throw SchemaVerificationException(s"Wildcard qualifier '$qualifier' refers to non-existent table.")
        }

    private def loadPhysicalColNames(tref: TableRef): List[String] =
        SchemaResolver.load(tref, ds()) match {
            case ok: Ok[Table, SchemaVerificationException] => List.from(ok.value().columns().map(_.name()))
            case err: Err[Table, SchemaVerificationException] => throw err.err()
        }

    private def validateCommon(sources: List[TableSource], refs: List[Ref]): Unit = {
        if (sources.isEmpty) {
            assert(refs.isEmpty, "Refs without sources")
            return
        }

        val indexes = refs.collect { case r: Ref.Indexed => r.position }
        if (indexes.toSet.size != indexes.size)
            throw SchemaVerificationException("Duplicate positional indexes found in context.")

        val names = sources.map(_.effectiveName)
        if (names.toSet.size != names.size)
            throw SchemaVerificationException("Duplicate table effective names found in context.")
    }

    private def validateNamedOnly(refs: List[Ref], errs: ListBuffer[String]): Unit =
        refs.foreach {
            case _: Ref.Named =>
            case _: Ref.Indexed => errs.addOne("DML context does not allow positional refs")
        }

    private def validateConflict(
                                    conflict: OnConflict,
                                    physical: Map[TableRef, Table],
                                    virtual: Map[String, List[String]],
                                    errs: ListBuffer[String]
                                ): Unit = {
        
        for (cref <- conflict.conflictCrefs) validateCref(cref, physical, virtual, None, errs)

        conflict match {
            case oc: OnConflict.UpdateSet => for (cref <- oc.updateCrefs) validateCref(cref, physical, virtual, None, errs)
            case _: OnConflict.DoNothing =>
        }
    }

    private def validateProjection(
                                      sources: List[TableSource],
                                      refs: List[Ref],
                                      physical: Map[TableRef, Table],
                                      virtual: Map[String, List[String]],
                                      errs: ListBuffer[String]
                                  ): Unit = {

        val trefByName = sources.map(s => s.effectiveName -> s).toMap
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
                    case Some(src: TableSource.Physical) => loadPhysicalCrefs(src.tref, crefs)
                    case Some(src: TableSource.Virtual)  => loadVirtualCrefs(src, crefs)
                    case None => throw SchemaVerificationException(s"Wildcard qualifier '${qw.qualifier()}' refers to non-existent table.")
                }

            case _: Projection.Wildcard =>
                sources.flatMap {
                    case src: TableSource.Physical => loadPhysicalCrefs(src.tref, crefs)
                    case src: TableSource.Virtual  => loadVirtualCrefs(src, crefs)
                }
        }

        if (names.toSet.size != names.size)
            throw SchemaVerificationException("Duplicate column names or aliases found in context.")

        for (cref <- crefs.toList) validateCref(cref, physical, virtual, None, errs)
    }

    @tailrec
    private def validateCref(
                                cref: ColumnRef,
                                physical: Map[TableRef, Table],
                                virtual: Map[String, List[String]],
                                outer: Option[Context],
                                errs: ListBuffer[String]
                            ): Unit = {

        val physicalMatches = physical.filter { case (tref, table) => table.column(cref, tref).isPresent }

        val virtualMatch = cref match {
            case vc: ColumnRef.VariableColumn => virtual.get(vc.variable()).exists(_.exists(_.equalsIgnoreCase(cref.name())))
            case col => virtual.values.exists(_.exists(_.equalsIgnoreCase(col.name())))
        }

        val totalMatches = physicalMatches.size + (if virtualMatch then 1 else 0)

        totalMatches match {
            case 0 => outer match {
                case Some(ctx) =>
                    val (outerPhysical, outerVirtual) = resolve(ctx.sources)
                    validateCref(cref, outerPhysical, outerVirtual, ctx.outer, errs)

                case None => errs.addOne(s"Column '$cref' not found in any table or subquery")
            }

            case 1 if physicalMatches.size == 1 =>
                val (tref, table) = physicalMatches.head
                val column        = table.column(cref, tref).get
                (cref.typeClass, column) match {
                    case (tpe: Type.Some, col: Column.Known) => validateTypes(cref, tpe.value(), col.sqlType(), errs)
                    case _ =>
                }

            case 1 =>

            case _ =>
                errs.addOne(s"Ambiguous column '$cref' found in multiple tables: ${physicalMatches.keys.map(_.name()).mkString(", ")}")
        }
    }

    private def validateTypes(
                                 cref: ColumnRef,
                                 javaType: Class[?],
                                 sqlType: SQLType,
                                 errs: mutable.ListBuffer[String]
                             ): Unit =

        TypeRegistry.info(javaType) match {
            case typeInfo: TypeInfoOk =>
                if (!typeInfo.sqlTypes().contains(sqlType))
                    errs.addOne(typeMismatchError(cref, javaType, sqlType, typeInfo.sqlTypes().asScala.toSet))

            case _: TypeInfo.None => errs.addOne(unsupportedType(cref, javaType))
        }

    private def loadPhysicalCrefs(tref: TableRef, crefs: mutable.ListBuffer[ColumnRef]): List[String] = {
        val cols = SchemaResolver.load(tref, ds()) match {
            case ok: Ok[Table, SchemaVerificationException]   => List.from(ok.value().columns().map(_.name()))
            case err: Err[Table, SchemaVerificationException] => throw err.err()
        }
        crefs.addAll(cols.map(s => ColumnRef.Base(s)))
        cols
    }

    private def loadVirtualCrefs(src: TableSource.Virtual, crefs: mutable.ListBuffer[ColumnRef]): List[String] = {
        val cols = extractVirtualCols(src.ctx)
        crefs.addAll(cols.map(s => ColumnRef.VariableBase(src.effectiveName, s)))
        cols
    }

    private def throwIfErrors(errs: ListBuffer[String]): Unit =
        if (errs.nonEmpty)
            throw new SchemaVerificationException(s"Schema validation failed:\n  - ${errs.mkString("\n  - ")}")

    private def ds() =
        Conf.INSTANCE.dataSource().orElseThrow(() => SchemaVerificationException("Cannot obtain a datasource"))

    private def tableNotFound(source: TableSource.Physical) = s"Table '${source.tref}' not found"

    private def typeMismatchError(cref: ColumnRef, javaType: Class[?], sqlType: SQLType, compatible: Set[SQLType]): String =
        s"Type mismatch for column '$cref': Java type '${javaType.getSimpleName}' is not compatible with SQL type '$sqlType'. Expected one of: $compatible"

    private def unsupportedType(cref: ColumnRef, javaType: Class[?]): String =
        s"Unsupported Java type '${javaType.getName}' for column '$cref'"
}

object ContextFactory {

    def insertContext(
                         sources: java.util.List[TableSource],
                         refs: java.util.List[Ref],
                         onConflict: java.util.Optional[OnConflict],
                         returning: java.util.List[Ref],
                         outer: java.util.Optional[Context]
                     ): Context.Insert = Context.Insert(

        sources.asScala.toList,
        refs.asScala.toList,
        if onConflict.isPresent then Some(onConflict.get) else None,
        returning.asScala.toList,
        if outer.isPresent then Some(outer.get) else None
    )

    def selectContext(
                         sources: java.util.List[TableSource],
                         refs: java.util.List[Ref],
                         where: java.util.Optional[Expr],
                         groupBy: java.util.List[Expr],
                         having: java.util.Optional[Expr],
                         orderBy: java.util.List[Expr],
                         outer: java.util.Optional[Context]
                     ): Context.Select = Context.Select(

        sources.asScala.toList,
        refs.asScala.toList,
        if where.isPresent then Some(where.get) else None,
        groupBy.asScala.toList,
        if having.isPresent then Some(having.get) else None,
        orderBy.asScala.toList,
        if outer.isPresent then Some(outer.get) else None
    )

    def updateContext(
                         sources: java.util.List[TableSource],
                         refs: java.util.List[Ref],
                         where: java.util.Optional[Expr],
                         returning: java.util.List[Ref],
                         outer: java.util.Optional[Context]
                     ): Context.Update = Context.Update(

        sources.asScala.toList,
        refs.asScala.toList,
        if where.isPresent then Some(where.get) else None,
        returning.asScala.toList,
        if outer.isPresent then Some(outer.get) else None
    )

    def deleteContext(
                         sources: java.util.List[TableSource],
                         where: java.util.Optional[Expr],
                         returning: java.util.List[Ref],
                         outer: java.util.Optional[Context]
                     ): Context.Delete = Context.Delete(

        sources.asScala.toList,
        if where.isPresent then Some(where.get) else None,
        returning.asScala.toList,
        if outer.isPresent then Some(outer.get) else None
    )
}