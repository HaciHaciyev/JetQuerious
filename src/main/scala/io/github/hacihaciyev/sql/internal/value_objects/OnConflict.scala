package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.expressions.ColumnRef

sealed trait OnConflict {
    def conflictCrefs: List[ColumnRef.Base]
}

object OnConflict {

    case class DoNothing(conflictCrefs: List[ColumnRef.Base]) extends OnConflict {
        require(conflictCrefs != null, "conflictCrefs cannot be null")
        require(conflictCrefs.nonEmpty, "At least one conflict column is required")
        require(!conflictCrefs.contains(null), "Conflict columns cannot contain null")
    }

    case class UpdateSet(conflictCrefs: List[ColumnRef.Base], updateCrefs: List[ColumnRef.Base]) extends OnConflict {
        require(conflictCrefs != null, "conflictCrefs cannot be null")
        require(conflictCrefs.nonEmpty, "At least one conflict column is required")
        require(!conflictCrefs.contains(null), "Conflict columns cannot contain null")

        require(updateCrefs != null, "updateCrefs cannot be null")
        require(!updateCrefs.contains(null), "Update columns cannot contain null")

        private val intersection: List[ColumnRef.Base] = conflictCrefs.flatMap(c =>
            updateCrefs.find(u => u.name.equalsIgnoreCase(c.name))
        )
        require(intersection.isEmpty, s"Cannot UPDATE conflict columns: ${intersection.map(_.name()).mkString(", ")}")
    }
}