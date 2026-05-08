package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.expressions.ColumnRef

case class InsertEntry(col: ColumnRef.Base, type_ : Class[?]) {
    require(col != null)
    require(type_ != null)
}

object InsertEntry {
    def of(col: ColumnRef.Base, type_ : Class[?]): InsertEntry = InsertEntry(col, type_)
}