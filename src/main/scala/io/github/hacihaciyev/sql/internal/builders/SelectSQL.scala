package io.github.hacihaciyev.sql.internal.builders

import io.github.hacihaciyev.sql.expressions.Expr
import io.github.hacihaciyev.sql.internal.ExprRenderer
import io.github.hacihaciyev.sql.internal.value_objects.{FromSource, JoinEntry, ForUpdate}
import io.github.hacihaciyev.sql.value_objects.{Limit, Offset, Projection, TableRef}

import scala.jdk.CollectionConverters.*

object SelectSQL {

    def build(
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
        forUpdate:   java.util.Optional[ForUpdate]
    ): String = {

        val sb = new StringBuilder

        sb ++= "SELECT"
        if distinct then sb ++= " DISTINCT"
        sb ++= " "
        sb ++= projections.asScala.map(renderProjection).mkString(", ")

        sb ++= s" FROM ${from.sql}"

        joins.asScala.foreach {
            case JoinEntry.Inner(src, on) => sb ++= s" JOIN ${src.sql} ON ${ExprRenderer.render(on)}"
            case JoinEntry.Left(src, on)  => sb ++= s" LEFT JOIN ${src.sql} ON ${ExprRenderer.render(on)}"
            case JoinEntry.Right(src, on) => sb ++= s" RIGHT JOIN ${src.sql} ON ${ExprRenderer.render(on)}"
            case JoinEntry.Full(src, on)  => sb ++= s" FULL JOIN ${src.sql} ON ${ExprRenderer.render(on)}"
            case JoinEntry.Cross(src)     => sb ++= s" CROSS JOIN ${src.sql}"
        }

        if where.isPresent then sb ++= s" WHERE ${ExprRenderer.render(where.get)}"

        val gb = groupBy.asScala.toList
        if gb.nonEmpty then sb ++= s" GROUP BY ${gb.map(ExprRenderer.render).mkString(", ")}"

        if having.isPresent then sb ++= s" HAVING ${ExprRenderer.render(having.get)}"

        val ob = orderBy.asScala.toList
        if ob.nonEmpty then sb ++= s" ORDER BY ${ob.map(ExprRenderer.render).mkString(", ")}"

        if limit.isPresent  then sb ++= s" LIMIT ${limit.get.value()}"
        if offset.isPresent then sb ++= s" OFFSET ${offset.get.value()}"

        renderForUpdate(sb, forUpdate)

        sb.toString
    }

    private def renderProjection(p: Projection): String = p match {
        case base: Projection.Base            => ExprRenderer.render(base.expr())
        case aliased: Projection.Aliased      => s"${ExprRenderer.render(aliased.expr())} AS ${aliased.alias()}"
        case _: Projection.Wildcard           => "*"
        case qw: Projection.QualifiedWildcard => s"${qw.qualifier()}.*"
    }

    private def renderForUpdate(sb: StringBuilder, forUpdate: java.util.Optional[ForUpdate]): Unit = {
        if forUpdate.isEmpty then return
        forUpdate.get match {
            case ForUpdate.Simple => sb ++= " FOR UPDATE"
    
            case ForUpdate.NoWait => sb ++= " FOR UPDATE NOWAIT"
    
            case ForUpdate.SkipLocked => sb ++= " FOR UPDATE SKIP LOCKED"
    
            case ForUpdate.Of(tables) => 
                val tableNames = tables.map {
                    case aliased: TableRef.Aliased => aliased.alias()
                    case base => base.name()
                }.mkString(", ")
                sb ++= s" FOR UPDATE OF $tableNames"
    
            case ForUpdate.OfNoWait(tables) =>
                val tableNames = tables.map {
                    case aliased: TableRef.Aliased => aliased.alias()
                    case base => base.name()
                }.mkString(", ")
                sb ++= s" FOR UPDATE OF $tableNames NOWAIT"
    
            case ForUpdate.OfSkipLocked(tables) =>
                val tableNames = tables.map {
                    case aliased: TableRef.Aliased => aliased.alias()
                    case base => base.name()
                }.mkString(", ")
                sb ++= s" FOR UPDATE OF $tableNames SKIP LOCKED"
        }
    }
}