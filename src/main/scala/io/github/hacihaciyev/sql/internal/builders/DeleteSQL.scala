package io.github.hacihaciyev.sql.internal.builders

import io.github.hacihaciyev.sql.expressions.Expr
import io.github.hacihaciyev.sql.value_objects.TableRef
import io.github.hacihaciyev.sql.internal.ExprRenderer

import scala.jdk.CollectionConverters.*

object DeleteSQL {

    def build(
        tref: TableRef,
        where: java.util.Optional[Expr],
        returning: java.util.List[String]
    ): String = {
    
        val ret = returning.asScala.toList
        val sb  = new StringBuilder

        sb ++= s"DELETE FROM $tref"

        if where.isPresent then sb ++= s" WHERE ${ExprRenderer.render(where.get)}"
        if ret.nonEmpty    then sb ++= s" RETURNING ${ret.mkString(", ")}"

        sb.toString
    }
}