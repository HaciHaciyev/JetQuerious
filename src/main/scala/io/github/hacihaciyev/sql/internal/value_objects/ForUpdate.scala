package io.github.hacihaciyev.sql.internal.value_objects

import io.github.hacihaciyev.sql.expressions.*
import io.github.hacihaciyev.sql.value_objects.TableRef
import scala.jdk.CollectionConverters.*
import scala.annotation.targetName

sealed trait ForUpdate {
    def tables: List[TableRef]
}

object ForUpdate {
    
    case object Simple extends ForUpdate {
        override def tables: List[TableRef] = List.empty
    }
    
    case object NoWait extends ForUpdate {
        override def tables: List[TableRef] = List.empty
    }
    
    case object SkipLocked extends ForUpdate {
        override def tables: List[TableRef] = List.empty
    }
    
    case class Of(tables: List[TableRef]) extends ForUpdate {
        require(tables.nonEmpty, "At least one table required for FOR UPDATE OF")
    }
    
    case class OfNoWait(tables: List[TableRef]) extends ForUpdate {
        require(tables.nonEmpty, "At least one table required for FOR UPDATE OF")
    }
    
    case class OfSkipLocked(tables: List[TableRef]) extends ForUpdate {
        require(tables.nonEmpty, "At least one table required for FOR UPDATE OF")
    }
    
    def simple(): ForUpdate = Simple
    
    def noWait(): ForUpdate = NoWait
    
    def skipLocked(): ForUpdate = SkipLocked
    
    def of(tables: java.util.List[TableRef]): ForUpdate = Of(tables.asScala.toList)
    
    def ofNoWait(tables: java.util.List[TableRef]): ForUpdate = OfNoWait(tables.asScala.toList)
    
    def ofSkipLocked(tables: java.util.List[TableRef]): ForUpdate = OfSkipLocked(tables.asScala.toList)
    
    def of(table: TableRef, more: TableRef*): ForUpdate = Of((table +: more).toList)
    
    def ofNoWait(table: TableRef, more: TableRef*): ForUpdate = OfNoWait((table +: more).toList)
    
    def ofSkipLocked(table: TableRef, more: TableRef*): ForUpdate = OfSkipLocked((table +: more).toList)
}