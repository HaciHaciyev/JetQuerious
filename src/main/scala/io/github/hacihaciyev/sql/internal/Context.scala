package io.github.hacihaciyev.sql.internal

import io.github.hacihaciyev.build_errors.SchemaVerificationException
import io.github.hacihaciyev.config.Conf
import io.github.hacihaciyev.sql.value_objects.{Projection, TableRef}
import io.github.hacihaciyev.sql.expressions.{BinaryOp, ColumnRef, Expr, ValueExpr}
import io.github.hacihaciyev.sql.expressions.ColumnRef.*
import io.github.hacihaciyev.sql.internal.schema.{Column, SchemaResolver, Table}
import io.github.hacihaciyev.sql.internal.value_objects.{Ref, TableSource, OnConflict, ParamType, ForUpdate, FromSource, JoinEntry, JoinedOn}
import io.github.hacihaciyev.sql.internal.builders.SelectSQL
import io.github.hacihaciyev.sql.value_objects.{Limit, Offset}
import io.github.hacihaciyev.types.SQLType
import io.github.hacihaciyev.util.{Err, Ok}
import io.github.hacihaciyev.types.internal.{TypeInfo, TypeInfoOk, TypeRegistry}
import io.github.hacihaciyev.sql.value_objects.UnionType

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import io.github.hacihaciyev.sql.expressions.Literal

sealed trait Context {
    def sources: List[TableSource]
    def refs: List[Ref]
    def outer: Option[Context]
    def paramTypes: List[ParamType]

    require(sources != null)
    require(refs != null)
    require(outer != null)
    require(!sources.contains(null))
    require(!refs.contains(null))

    validate()

    protected def validate(): Unit

    def projectionColumns: java.util.List[ColumnRef] = {
        val crefs = mutable.ListBuffer[ColumnRef]()
        Context.effectiveProjectionNames(refs, sources, crefs)
        crefs.toList.asJava
    }
}

sealed trait DQL

sealed trait DML

case class CriteriaUnit(kind: ClauseKind, expr: Expr, optional: Boolean) {
    require(kind != null, "Clause kind cannot be null")
    require(expr != null, "Unit expression cannot be null")

    val paramTypes: List[Class[?]] = ExprTraversal.collectPlaceholders(expr)

    require(
        !optional || paramTypes.size == 1,
        s"SQL.opt(...) must be the sole placeholder within its ${kind} unit, found ${paramTypes.size}"
    )
}

case class Prepared(sql: String, paramTypes: java.util.List[ParamType], args: Array[AnyRef])

object Context {

    case class Select(
                         sources: List[TableSource],
                         refs: List[Ref],
                         joins: List[Expr],
                         where: Option[Expr],
                         groupBy: List[Expr],
                         having: Option[Expr],
                         orderBy: List[Expr],
                         forUpdate: List[TableRef] = List.empty,
                         outer: Option[Context] = None
                     ) extends Context, DQL {

        req(sources, refs, joins, where, groupBy, having, orderBy, outer)

        def paramTypes: List[ParamType] = withPositions(ExprTraversal.collectAllPlaceholders(
            ExprTraversal.refsToExprsExcludingWildcards(refs) ++
            joins                                             ++
            where.toList                                      ++
            groupBy                                           ++
            having.toList                                     ++
            orderBy
        ))

        protected def validate(): Unit = {
            validateCommon(sources, refs)
            val (physical, virtual) = resolve(sources)
            val errs = mutable.ListBuffer[String]()

            validateProjection(sources, refs, physical, virtual, outer, errs)

            validateForUpdateTables(physical, errs)

            val whereCrefs  = where.toList.flatMap(ExprTraversal.collectCrefs)
            val groupCrefs  = groupBy.flatMap(ExprTraversal.collectCrefs)
            val havingCrefs = having.toList.flatMap(ExprTraversal.collectCrefs)
            val orderCrefs  = orderBy.flatMap(ExprTraversal.collectCrefs)
            val joinsCrefs  = joins.flatMap(ExprTraversal.collectCrefs)

            val extraCrefs  = whereCrefs ++ groupCrefs ++ havingCrefs ++ orderCrefs ++ joinsCrefs

            for (cref <- extraCrefs) validateCref(cref, physical, virtual, outer, errs)
            throwIfErrors(errs)
        }

        private def validateForUpdateTables(physical: Map[TableRef, Table], errs: ListBuffer[String]): Unit = {
            if forUpdate.isEmpty then return

            val physicalTableRefs = physical.keys.toSet

            forUpdate.foreach { tableRef =>
                if !physicalTableRefs.exists(_.equals(tableRef)) then errs.addOne(s"Table '$tableRef' in FOR UPDATE OF not found in sources")
            }
        }
    }

    case class Insert(
                         sources: List[TableSource],
                         refs: List[Ref],
                         conflict: Option[OnConflict],
                         returning: List[Ref],
                         outer: Option[Context] = None
                     ) extends Context, DML {

        req(sources, refs, conflict, returning, outer)

        def paramTypes: List[ParamType] = withPositions(collectRefTypes(refs))

        protected def validate(): Unit = {
            validateCommon(sources, refs)
            val (physical, virtual) = resolve(sources)
            val errs = mutable.ListBuffer[String]()

            validateNamedOnly(refs, errs)
            validateProjection(sources, refs, physical, virtual, outer, errs)

            conflict.foreach(c => validateConflict(c, physical, virtual, errs))

            if returning.nonEmpty then validateProjection(sources, returning, physical, virtual, outer, errs)
            throwIfErrors(errs)
        }
    }

    case class Update(
                         sources: List[TableSource],
                         refs: List[Ref],
                         setExprs: List[Expr],
                         where: Option[Expr],
                         returning: List[Ref],
                         outer: Option[Context] = None
                     ) extends Context, DML {

        req(sources, refs, setExprs, where, returning, outer)

        def paramTypes: List[ParamType] = {
            var setExprIdx = 0
            val setTypes = refs.flatMap {
                case Ref.Named(pb: Projection.Base) if pb.expr.isInstanceOf[ColumnRef] =>
                    pb.expr.asInstanceOf[ColumnRef].typeClass() match {
                        case t: ColumnRef.Type.Some => List(t.value())
                        case _: ColumnRef.Type.None =>
                            val types = ExprTraversal.collectPlaceholders(setExprs(setExprIdx))
                            setExprIdx += 1
                            types
                    }

                case _ => List()
            }
            withPositions(setTypes ++ ExprTraversal.collectAllPlaceholders(where.toList))
        }

        protected def validate(): Unit = {
            validateCommon(sources, refs)
            val (physical, virtual) = resolve(sources)
            val errs = mutable.ListBuffer[String]()

            validateNamedOnly(refs, errs)
            validateProjection(sources, refs, physical, virtual, outer, errs)

            val extraCrefs = where.toList.flatMap(ExprTraversal.collectCrefs) ++ setExprs.toList.flatMap(ExprTraversal.collectCrefs)
            for (cref <- extraCrefs) validateCref(cref, physical, virtual, outer, errs)

            if returning.nonEmpty then validateProjection(sources, returning, physical, virtual, outer, errs)
            throwIfErrors(errs)
        }
    }

    case class Delete(
                         sources: List[TableSource],
                         where: Option[Expr],
                         returning: List[Ref],
                         outer: Option[Context] = None
                     ) extends Context, DML {

        req(sources, where, returning, outer)

        override def refs: List[Ref] = List.empty

        def paramTypes: List[ParamType] = withPositions(ExprTraversal.collectAllPlaceholders(where.toList))

        protected def validate(): Unit = {
            validateCommon(sources, refs)
            val (physical, virtual) = resolve(sources)
            val errs = mutable.ListBuffer[String]()

            val whereCrefs = where.toList.flatMap(ExprTraversal.collectCrefs)
            for (cref <- whereCrefs) validateCref(cref, physical, virtual, outer, errs)

            if returning.nonEmpty then validateProjection(sources, returning, physical, virtual, outer, errs)
            throwIfErrors(errs)
        }
    }

    case class Union(
                       queries:   List[Context & DQL],
                       unionType: UnionType,
                       orderBy:   List[Expr],
                       outer:     Option[Context] = None
                   ) extends Context, DQL {

        req(queries, unionType, orderBy, outer)
        require(queries.size >= 2, "UNION requires at least two queries")

        override def sources: List[TableSource] = queries.head.sources
        override def refs: List[Ref]            = queries.head.refs

        def paramTypes: List[ParamType] = withPositions(
            queries.flatMap(_.paramTypes.map(_._type)) ++
            ExprTraversal.collectAllPlaceholders(orderBy)
        )

        protected def validate(): Unit = {
            validateColumnCounts()
            validateOrderBy()
        }

        private def validateColumnCounts(): Unit = {
            val firstSize = effectiveProjectionNames(queries.head.refs, queries.head.sources).size +
                            queries.head.refs.count(_.isInstanceOf[Ref.Indexed])

            queries.tail.foreach { q =>
                val size = effectiveProjectionNames(q.refs, q.sources).size + q.refs.count(_.isInstanceOf[Ref.Indexed])

                if (size != firstSize) {
                    throw SchemaVerificationException(s"All queries in UNION must have the same number of columns. First query has $firstSize, but another has $size")
                }
            }
        }

        private def validateOrderBy(): Unit = {
            if orderBy.isEmpty then return

            val known      = effectiveProjectionNames(queries.head.refs, queries.head.sources).map(_.toLowerCase).toSet
            val errs       = mutable.ListBuffer[String]()
            val orderCrefs = orderBy.flatMap(ExprTraversal.collectCrefs)

            for (cref <- orderCrefs) {
                if !known.exists(_.equalsIgnoreCase(cref.name())) then errs.addOne(s"ORDER BY column '$cref' not found in UNION projection")
            }

            throwIfErrors(errs)
        }
    }

    case class Criteria(
                           projections: List[Projection],
                           distinct:    Boolean,
                           source:      FromSource,
                           joinEntries: List[JoinEntry],
                           units:       List[CriteriaUnit],
                           limit:       Option[Limit]     = None,
                           offset:      Option[Offset]    = None,
                           forUpdate:   Option[ForUpdate] = None,
                           outer:       Option[Context]   = None
                       ) extends Context, DQL {

        req(projections, source, joinEntries, units, limit, offset, forUpdate, outer)
        require(projections.nonEmpty, "At least one projection is required")

        override def sources: List[TableSource] =
            source.toTableSource :: joinEntries.map(_.source.toTableSource)

        override def refs: List[Ref] = projections.map(Ref.Named.apply)

        def joins: List[Expr] = joinEntries.collect { case j: JoinedOn => j.on }

        def where: Option[Expr]  = foldAnd(exprsOf(units, ClauseKind.WHERE))
        def groupBy: List[Expr]  = exprsOf(units, ClauseKind.GROUP_BY)
        def having: Option[Expr] = foldAnd(exprsOf(units, ClauseKind.HAVING))
        def orderBy: List[Expr]  = exprsOf(units, ClauseKind.ORDER_BY)

        def fixedTypes: List[Class[?]] =
            ExprTraversal.collectAllPlaceholders(ExprTraversal.refsToExprsExcludingWildcards(refs) ++ joins)

        def paramTypes: List[ParamType] = withPositions(fixedTypes ++ units.flatMap(_.paramTypes))

        def unitsJava: java.util.List[CriteriaUnit] = units.asJava

        def paramTypesJava: java.util.List[ParamType] = paramTypes.asJava

        def unitsOf(kind: ClauseKind): java.util.List[CriteriaUnit] = units.filter(_.kind == kind).asJava

        def prepare(args: Array[AnyRef]): Prepared = {
            val supplied = if args == null then 0 else args.length
            val declared = paramTypes.size
            if (supplied != declared)
                throw new IllegalArgumentException(s"Criteria query expects $declared argument(s) but $supplied were provided")

            val fixed    = fixedTypes
            val keptArgs = mutable.ListBuffer[AnyRef]()
            val kept     = mutable.ListBuffer[CriteriaUnit]()

            keptArgs ++= args.take(fixed.size)
            var i = fixed.size

            units.foreach { unit =>
                val slice = args.slice(i, i + unit.paramTypes.size)
                i += unit.paramTypes.size

                if (!(unit.optional && slice(0) == null)) {
                    kept     += unit
                    keptArgs ++= slice
                }
            }

            val active = kept.toList

            val sql = SelectSQL.build(
                projections.asJava,
                distinct,
                source,
                joinEntries.asJava,
                toOptional(foldAnd(exprsOf(active, ClauseKind.WHERE))),
                exprsOf(active, ClauseKind.GROUP_BY).asJava,
                toOptional(foldAnd(exprsOf(active, ClauseKind.HAVING))),
                exprsOf(active, ClauseKind.ORDER_BY).asJava,
                toOptional(limit),
                toOptional(offset),
                toOptional(forUpdate)
            )

            Prepared(sql, withPositions(fixed ++ active.flatMap(_.paramTypes)).asJava, keptArgs.toArray)
        }

        protected def validate(): Unit = {
            val errs = mutable.ListBuffer[String]()

            for (expr <- ExprTraversal.refsToExprsExcludingWildcards(refs) ++ joins)
                if CriteriaSupport.containsOptional(expr) then
                    errs.addOne("SQL.opt(...) is only allowed in WHERE, GROUP BY, HAVING or ORDER BY, not in SELECT or JOIN ON")

            throwIfErrors(errs)

            val _ = Context.Select(
                sources, refs, joins, where, groupBy, having, orderBy,
                forUpdate.map(_.tables).getOrElse(List.empty), outer
            )
        }
    }

    private def exprsOf(units: List[CriteriaUnit], kind: ClauseKind): List[Expr] =
        units.filter(_.kind == kind).map(_.expr)

    private def foldAnd(exprs: List[Expr]): Option[Expr] =
        exprs.reduceOption((l, r) => BinaryOp(BinaryOp.BinaryOperator.AND, l, r))

    private def toOptional[A](value: Option[A]): java.util.Optional[A] =
        value.fold(java.util.Optional.empty[A]())(java.util.Optional.of)

    private def collectRefTypes(refs: List[Ref]): List[Class[?]] =
        refs.flatMap {
            case Ref.Named(pb: Projection.Base) if pb.expr.isInstanceOf[ColumnRef] =>
                pb.expr.asInstanceOf[ColumnRef].typeClass() match {
                    case t: ColumnRef.Type.Some => List(t.value())
                    case _: ColumnRef.Type.None => List()
                }
            case _ => List()
        }

    case class CTE(
                       entries: List[Context],
                       inner:   Context & DQL
                   ) extends Context, DQL {

        req(entries, inner)

        override def sources: List[TableSource] = inner.sources
        override def refs: List[Ref]            = inner.refs
        override def outer: Option[Context]     = inner.outer

        def paramTypes: List[ParamType] = withPositions(
            entries.flatMap(_.paramTypes.map(_._type)) ++
            inner.paramTypes.map(_._type)
        )

        protected def validate(): Unit = ()
    }

    case class CTEWrite(
                            entries: List[Context],
                            inner:   Context & DML
                        ) extends Context, DML {

        req(entries, inner)

        override def sources: List[TableSource] = inner.sources
        override def refs: List[Ref]            = inner.refs
        override def outer: Option[Context]     = inner.outer

        def paramTypes: List[ParamType] = withPositions(
            entries.flatMap(_.paramTypes.map(_._type)) ++
            inner.paramTypes.map(_._type)
        )

        protected def validate(): Unit = ()
    }

    private def withPositions(types: List[Class[?]]): List[ParamType] = types.zipWithIndex.map((t, i) => ParamType(i + 1, t))

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

    private def effectiveProjectionNames(refs: List[Ref], sources: List[TableSource], crefs: mutable.ListBuffer[ColumnRef] = mutable.ListBuffer[ColumnRef]()): List[String] = {
        val trefByName = sources.map(s => s.effectiveName -> s).toMap

        refs.map(_.value).flatMap {
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
                                      outer: Option[Context],
                                      errs: ListBuffer[String]
                                  ): Unit = {

        val crefs = mutable.ListBuffer[ColumnRef]()
        val names = effectiveProjectionNames(refs, sources, crefs)
        if names.toSet.size != names.size then throw SchemaVerificationException("Duplicate column names or aliases found in context.")
        for (cref <- crefs.toList) validateCref(cref, physical, virtual, outer, errs)
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
        val effectiveName = tref match {
            case ta: TableRef.Aliased => ta.alias()
            case base                 => base.name()
        }
        val cols = SchemaResolver.load(tref, ds()) match {
            case ok: Ok[Table, SchemaVerificationException]   => List.from(ok.value().columns().map(_.name()))
            case err: Err[Table, SchemaVerificationException] => throw err.err()
        }
        crefs.addAll(cols.map(s => ColumnRef.VariableBase(effectiveName, s)))
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

    private def req(values: Any*): Unit =
        values.foreach {
            case null            => require(false, "null value")
            case opt: Option[?]  => require(opt.forall(_ != null), "Option contains null")
            case xs: Iterable[?] => require(!xs.exists(_ == null), "Collection contains null")
            case _               =>
        }
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
                         joins: java.util.List[Expr],
                         where: java.util.Optional[Expr],
                         groupBy: java.util.List[Expr],
                         having: java.util.Optional[Expr],
                         orderBy: java.util.List[Expr],
                         forUpdate: java.util.Optional[ForUpdate],
                         outer: java.util.Optional[Context]
                     ): Context.Select = Context.Select(

        sources.asScala.toList,
        refs.asScala.toList,
        joins.asScala.toList,
        if where.isPresent then Some(where.get) else None,
        groupBy.asScala.toList,
        if having.isPresent then Some(having.get) else None,
        orderBy.asScala.toList,
        if forUpdate.isPresent then forUpdate.get.tables else List.empty,
        if outer.isPresent then Some(outer.get) else None
    )

    def updateContext(
                         sources: java.util.List[TableSource],
                         refs: java.util.List[Ref],
                         setExprs: java.util.List[Expr],
                         where: java.util.Optional[Expr],
                         returning: java.util.List[Ref],
                         outer: java.util.Optional[Context]
                     ): Context.Update = Context.Update(

        sources.asScala.toList,
        refs.asScala.toList,
        setExprs.asScala.toList,
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

    def unionContext(
                       queries:   java.util.List[Context],
                       unionType: UnionType,
                       orderBy:   java.util.List[Expr],
                       outer:     java.util.Optional[Context]
                   ): Context.Union = Context.Union(

        queries.asScala.toList.map(_.asInstanceOf[Context & DQL]),
        unionType,
        orderBy.asScala.toList,
        if outer.isPresent then Some(outer.get) else None
    )

    def criteriaContext(
                           projections: java.util.List[Projection],
                           distinct:    Boolean,
                           from:        FromSource,
                           joins:       java.util.List[JoinEntry],
                           where:       java.util.Optional[Expr],
                           groupBy:     java.util.List[Expr],
                           having:      java.util.Optional[Expr],
                           orderBy:     java.util.List[Expr],
                           limit:       java.util.Optional[Limit],
                           offset:      java.util.Optional[Offset],
                           forUpdate:   java.util.Optional[ForUpdate],
                           outer:       java.util.Optional[Context]
                       ): Context.Criteria = Context.Criteria(

        projections.asScala.toList,
        distinct,
        from,
        joins.asScala.toList,
        conjunctUnits(where, ClauseKind.WHERE)     ++
        itemUnits(groupBy, ClauseKind.GROUP_BY)     ++
        conjunctUnits(having, ClauseKind.HAVING)   ++
        itemUnits(orderBy, ClauseKind.ORDER_BY),
        asScalaOption(limit),
        asScalaOption(offset),
        asScalaOption(forUpdate),
        asScalaOption(outer)
    )

    private def conjunctUnits(expr: java.util.Optional[Expr], kind: ClauseKind): List[CriteriaUnit] =
        if !expr.isPresent then List.empty
        else CriteriaSupport.flattenAnd(expr.get).map(c => CriteriaUnit(kind, c, CriteriaSupport.containsOptional(c)))

    private def itemUnits(items: java.util.List[Expr], kind: ClauseKind): List[CriteriaUnit] =
        items.asScala.toList.map(item => CriteriaUnit(kind, item, CriteriaSupport.containsOptional(item)))

    private def asScalaOption[A](value: java.util.Optional[A]): Option[A] =
        if value.isPresent then Some(value.get) else None

    def cteReadContext(
                           entries: java.util.List[Context],
                           inner:   Context
                       ): Context.CTE = Context.CTE(
        entries.asScala.toList,
        inner.asInstanceOf[Context & DQL]
    )

    def cteWriteContext(
                            entries: java.util.List[Context],
                            inner:   Context
                        ): Context.CTEWrite = Context.CTEWrite(
        entries.asScala.toList,
        inner.asInstanceOf[Context & DML]
    )
}