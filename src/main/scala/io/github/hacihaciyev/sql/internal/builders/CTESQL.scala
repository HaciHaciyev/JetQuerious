package io.github.hacihaciyev.sql.internal.builders

import io.github.hacihaciyev.sql.JQ
import io.github.hacihaciyev.sql.value_objects.UnionType
import io.github.hacihaciyev.sql.internal.value_objects.CTEEntry

import scala.jdk.CollectionConverters.*

object CTESQL {

    def build(
        entries:    java.util.List[CTEEntry],
        finalQuery: JQ
    ): String = {
        
        val sb = new StringBuilder
        sb ++= "WITH "

        val hasRecursive = entries.asScala.exists(_.isInstanceOf[CTEEntry.Recursive])
        if hasRecursive then sb ++= "RECURSIVE "

        sb ++= entries.asScala.map(_.sql).mkString(", ")
        sb ++= s" ${finalQuery.sql()}"

        sb.toString
    }
}