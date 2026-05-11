package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.JQ
import io.github.hacihaciyev.sql.value_objects.TableRef

sealed trait FromSource {
    def toTableSource: TableSource
    def sql: String
}

object FromSource {

    case class Physical(tref: TableRef) extends FromSource {
        require(tref != null, "TableRef cannot be null")

        def toTableSource: TableSource = TableSource.Physical(tref)
        def sql: String = tref.toString
    }

    case class Subquery(query: JQ.Read, alias: String) extends FromSource {
        require(query != null, "Subquery cannot be null")
        require(alias != null && alias.nonEmpty, "Subquery alias cannot be blank")

        def toTableSource: TableSource = TableSource.Virtual(alias, Some(alias), query.context().asInstanceOf[io.github.hacihaciyev.sql.internal.Context])
        def sql: String = s"(${query.sql()}) AS $alias"
    }
}