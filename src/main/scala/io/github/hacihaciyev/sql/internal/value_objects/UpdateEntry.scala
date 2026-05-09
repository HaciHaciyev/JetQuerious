package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.expressions.{ColumnRef, Expr}

sealed trait UpdateEntry {
    def col: ColumnRef.Base
}

object UpdateEntry {

    case class Param(col: ColumnRef.Base, type_ : Class[?]) extends UpdateEntry {
        require(col != null)
        require(type_ != null)
    }

    case class Computed(col: ColumnRef.Base, expr: Expr) extends UpdateEntry {
        require(col != null)
        require(expr != null)
    }

    def param(col: ColumnRef.Base, type_ : Class[?]): Param       = Param(col, type_)
    def computed(col: ColumnRef.Base, expr: Expr): Computed       = Computed(col, expr)
}