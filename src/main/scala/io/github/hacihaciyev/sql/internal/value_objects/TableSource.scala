package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.value_objects.TableRef
import io.github.hacihaciyev.sql.internal.Context

sealed trait TableSource {
    def effectiveName: String
}

object TableSource {

    case class Physical(tref: TableRef) extends TableSource {
        require(tref != null)

        def effectiveName: String = tref match {
            case ta: TableRef.Aliased => ta.alias()
            case base                 => base.name()
        }
    }

    case class Virtual(name: String, alias: Option[String], ctx: Context) extends TableSource {
        require(name != null && name.nonEmpty && !name.isBlank, "Virtual table name cannot be blank")
        require(alias != null)
        require(
            alias.forall(a => a != null && a.nonEmpty && !a.isBlank),
            "Alias cannot be null, empty or blank"
        )
        require(ctx != null)

        def effectiveName: String = alias.getOrElse(name)
    }
}