package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.value_objects.Projection

sealed trait Ref {
    def value: Projection
}

object Ref {
    case class Named(value: Projection) extends Ref {
        require(value != null)
    }

    case class Indexed(value: Projection, position: Int) extends Ref {
        require(value != null)
        require(position > 0)
    }
}
