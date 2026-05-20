package io.github.hacihaciyev.sql.internal.builders

import io.github.hacihaciyev.sql.expressions.Expr
import io.github.hacihaciyev.sql.internal.ExprRenderer
import io.github.hacihaciyev.sql.value_objects.{Limit, Offset, UnionType}

import scala.jdk.CollectionConverters.*

object UnionSQL {

    def build(
        unionType: UnionType,
        queries:   java.util.List[String],
        orderBy:   java.util.List[Expr],
        limit:     java.util.Optional[Limit],
        offset:    java.util.Optional[Offset]
    ): String = {
    
        val keyword = unionType match {
            case UnionType.UNION     => " UNION "
            case UnionType.UNION_ALL => " UNION ALL "
            case UnionType.INTERSECT => " INTERSECT "
            case UnionType.EXCEPT    => " EXCEPT "
        }

        val sb = new StringBuilder
        sb ++= queries.asScala.mkString(keyword)

        val ob = orderBy.asScala.toList
        if ob.nonEmpty then sb ++= s" ORDER BY ${ob.map(ExprRenderer.render).mkString(", ")}"

        if limit.isPresent  then sb ++= s" LIMIT ${limit.get.value()}"
        if offset.isPresent then sb ++= s" OFFSET ${offset.get.value()}"

        sb.toString
    }
}