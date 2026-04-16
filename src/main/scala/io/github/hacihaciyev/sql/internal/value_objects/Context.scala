package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.build_errors.SchemaVerificationException
import io.github.hacihaciyev.config.Conf
import io.github.hacihaciyev.sql.value_objects.{Projection, TableRef}
import io.github.hacihaciyev.sql.expressions.{ColumnRef, ValueExpr}
import io.github.hacihaciyev.sql.expressions.ColumnRef.*
import io.github.hacihaciyev.sql.internal.schema.{Column, SchemaResolver, Table}
import io.github.hacihaciyev.types.SQLType
import io.github.hacihaciyev.util.{Err, Ok}
import io.github.hacihaciyev.types.internal.{TypeInfo, TypeInfoOk, TypeRegistry}

import scala.jdk.CollectionConverters.*
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class Context(trefs: List[TableRef], refs: List[Ref]) {
    require(trefs != null)
    require(refs != null)
    require(!trefs.contains(null))
    require(!refs.contains(null))

    validate()

    private def validate(): Unit = {
        if (trefs.isEmpty) {
            assert(refs.isEmpty, "ColumnRefs without TableRefs")
            return
        }
        
        val indexes = refs.collect { 
            case Ref.Indexed(_, pos) => pos 
        }
        if (indexes.toSet.size != indexes.size) {
            throw SchemaVerificationException("Duplicate positional indexes found in context.")
        }

        val effectiveTrefNames = trefs.map {
            case ta: TableRef.Aliased => ta.alias()
            case base => base.name()
        }
        if (effectiveTrefNames.toSet.size != effectiveTrefNames.size) {
            throw SchemaVerificationException("Duplicate table effective names found in context.")
        }

        val crefs = mutable.ListBuffer[ColumnRef]()
        val trefByName = effectiveTrefNames.zip(trefs).toMap

        val effectiveNames = refs.map(_.value).flatMap {
            case base: Projection.Base => base.expr match {
                case ac: ColumnRef.AliasedColumn => {
                    crefs.addOne(ac match {
                        case a: Alias => a
                        case va: VariableAlias => va
                    })
                    List(ac.alias())
                }
                case col: ColumnRef => {
                    crefs.addOne(col)
                    List(col.name())
                }    
                case _: ValueExpr => List()
            }

            case aliased: Projection.Aliased => List(aliased.alias())

            case qw: Projection.QualifiedWildcard => trefByName.get(qw.qualifier) match {
                case Some(tref) => loadCrefs(tref, crefs)
                case None => throw SchemaVerificationException(s"Wildcard qualifier '${qw.qualifier}' refers to non-existent table.")
            }    

            case _: Projection.Wildcard => trefs.flatMap(tref => loadCrefs(tref, crefs))
        }
        if (effectiveNames.toSet.size != effectiveNames.size) {
            throw SchemaVerificationException("Duplicate column names or aliases found in context.")
        }

        validateSchema(crefs.toList)
    }

    private def validateSchema(crefs: List[ColumnRef]): Unit = {
        val tables = trefs.flatMap { tref => SchemaResolver.load(tref, ds()) match {
            case ok: Ok[Table, SchemaVerificationException] => Some(tref -> ok.value())
            case err: Err[Table, SchemaVerificationException] => throw SchemaVerificationException(s"Table '$tref' not found", err.err())
        }}.toMap

        val errs = mutable.ListBuffer[String]()
        for (cref <- crefs) validateCref(cref, tables, errs)

        if (errs.nonEmpty) {
            throw new SchemaVerificationException(s"Schema validation failed:\n  - ${errs.mkString("\n  - ")}")
        }
    }

    private def validateCref(cref: ColumnRef, tables: Map[TableRef, Table], errs: ListBuffer[String]) = {
        tables.exists { case (tref, table) =>
            val columnOpt = table.column(cref, tref)

            if (columnOpt.isPresent) {
                val column = columnOpt.get

                (cref.typeClass, column) match {
                    case (tpe: Type.Some, col: Column.Known) => validateTypes(cref, tpe.value, col.sqlType(), errs)
                    case _ =>
                }

                true
            }
            else {
                errs.addOne(cref.toString)
                false
            }
        }
    }

    private def validateTypes(cref: ColumnRef, javaType: Class[?], sqlType: SQLType, errs: mutable.ListBuffer[String]): Unit = {
        TypeRegistry.info(javaType) match {
            case typeInfo: TypeInfoOk => {
                if (!typeInfo.sqlTypes().contains(sqlType)) {
                    errs.addOne(typeMismatchError(cref, javaType, sqlType, typeInfo.sqlTypes().asScala.toSet))
                }
            }
            case none : TypeInfo.None => errs.addOne(unsupportedType(cref, javaType))
        }
    }

    private def loadCrefs(tref: TableRef, crefs: mutable.ListBuffer[ColumnRef]): List[String] = {
        val trefCols = SchemaResolver.load(tref, ds()) match {
            case ok: Ok[Table, SchemaVerificationException] => List.from(ok.value().columns().map(_.name()))
            case err: Err[Table, SchemaVerificationException] => throw err.err()
        }
        crefs.addAll(trefCols.map(s => ColumnRef.Base(s)))
        trefCols
    }

    private def ds() = Conf.INSTANCE.dataSource().orElseThrow(() => SchemaVerificationException("Cannot obtain a datasource"))

    private def typeMismatchError(cref: ColumnRef, javaType: Class[?], sqlType: SQLType, compatible: Set[SQLType]): String =
        s"Type mismatch for column '$cref': Java value '${javaType.getSimpleName}' is not compatible with SQL value '$sqlType'. Expected one of: $compatible"

    private def unsupportedType(cref: ColumnRef, javaType: Class[?]): String = s"Unsupported Java value '${javaType.getName}' for column '$cref'"
}