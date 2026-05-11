package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.expressions.Expr

sealed trait JoinEntry {
    def source: FromSource
}

object JoinEntry {

    case class Inner(source: FromSource, on: Expr) extends JoinEntry {
        require(source != null, "JOIN source cannot be null")
        require(on != null,     "JOIN ON condition cannot be null")
    }

    case class Left(source: FromSource, on: Expr) extends JoinEntry {
        require(source != null, "LEFT JOIN source cannot be null")
        require(on != null,     "LEFT JOIN ON condition cannot be null")
    }

    case class Right(source: FromSource, on: Expr) extends JoinEntry {
        require(source != null, "RIGHT JOIN source cannot be null")
        require(on != null,     "RIGHT JOIN ON condition cannot be null")
    }

    case class Full(source: FromSource, on: Expr) extends JoinEntry {
        require(source != null, "FULL JOIN source cannot be null")
        require(on != null,     "FULL JOIN ON condition cannot be null")
    }

    case class Cross(source: FromSource) extends JoinEntry {
        require(source != null, "CROSS JOIN source cannot be null")
    }

    def inner(source: FromSource, on: Expr): Inner = Inner(source, on)
    def left(source: FromSource, on: Expr): Left   = Left(source, on)
    def right(source: FromSource, on: Expr): Right  = Right(source, on)
    def full(source: FromSource, on: Expr): Full   = Full(source, on)
    def cross(source: FromSource): Cross            = Cross(source)
}