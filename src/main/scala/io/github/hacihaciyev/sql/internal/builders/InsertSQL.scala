package io.github.hacihaciyev.sql.internal.builders

import io.github.hacihaciyev.sql.internal.value_objects.InsertEntry
import io.github.hacihaciyev.sql.internal.value_objects.OnConflict
import io.github.hacihaciyev.sql.value_objects.TableRef

import scala.jdk.CollectionConverters.*

object InsertSQL {

    def build(
        tref: TableRef,
        entries: java.util.List[InsertEntry],
        conflict: java.util.Optional[OnConflict],
        returning: java.util.List[String]
    ): String = {
        
        val cols = entries.asScala.toList.map(_.col.name())
        val ret  = returning.asScala.toList
        val oc   = if conflict.isPresent then Some(conflict.get) else None

        val sb = new StringBuilder
        sb ++= s"INSERT INTO $tref"
        sb ++= s" (${cols.mkString(", ")})"
        sb ++= s" VALUES (${List.fill(cols.size)("?").mkString(", ")})"

        oc.foreach {
            case OnConflict.DoNothing(crefs) =>
                sb ++= s" ON CONFLICT (${crefs.map(_.name()).mkString(", ")}) DO NOTHING"

            case OnConflict.UpdateSet(crefs, updates) =>
                sb ++= s" ON CONFLICT (${crefs.map(_.name()).mkString(", ")})"
                sb ++= s" DO UPDATE SET ${updates.map(c => s"${c.name()} = EXCLUDED.${c.name()}").mkString(", ")}"
        }

        if ret.nonEmpty then sb ++= s" RETURNING ${ret.mkString(", ")}"

        sb.toString
    }
}