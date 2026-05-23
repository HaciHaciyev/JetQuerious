package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.JQ;
import io.github.hacihaciyev.sql.value_objects.UnionType;

sealed trait CTEEntry {
    def name: String
    def sql: String
}

object CTEEntry {
    case class Regular(name: String, query: JQ) extends CTEEntry {
        def sql: String = s"$name AS (${query.sql()})"
    }
    
    case class Recursive(name: String, base: JQ.Read, recursive: JQ.Read, unionType: UnionType) extends CTEEntry {
        def sql: String =
            val keyword = unionType match {
                case UnionType.UNION     => "UNION"
                case UnionType.UNION_ALL => "UNION ALL"
                case UnionType.INTERSECT => "INTERSECT"
                case UnionType.EXCEPT    => "EXCEPT"
            }
            s"$name AS (${base.sql()} $keyword ${recursive.sql()})"
    }
}
