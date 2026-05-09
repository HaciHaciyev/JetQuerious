package io.github.hacihaciyev.sql.internal.builders

import io.github.hacihaciyev.sql.expressions.Expr
import io.github.hacihaciyev.sql.internal.value_objects.UpdateEntry
import io.github.hacihaciyev.sql.value_objects.TableRef
import io.github.hacihaciyev.sql.internal.ExprRenderer

import scala.jdk.CollectionConverters.*

object UpdateSQL {

    def build(
        tref: TableRef,
        entries: java.util.List[UpdateEntry],
        where: java.util.Optional[Expr],
        returning: java.util.List[String]
    ): String = {
        
        val ret = returning.asScala.toList
        val sb  = new StringBuilder

        sb ++= s"UPDATE $tref SET "
        sb ++= entries.asScala.map {
            case UpdateEntry.Param(col, _)       => s"${col.name()} = ?"
            case UpdateEntry.Computed(col, expr) => s"${col.name()} = ${ExprRenderer.render(expr)}"
        }.mkString(", ")

        if where.isPresent then sb ++= s" WHERE ${ExprRenderer.render(where.get)}"
        if ret.nonEmpty    then sb ++= s" RETURNING ${ret.mkString(", ")}"

        sb.toString
    }
}