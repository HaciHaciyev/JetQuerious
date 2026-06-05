package io.github.hacihaciyev.sql.internal.value_objects

import scala.jdk.CollectionConverters.*

sealed trait ForUpdate {
    def sqlSuffix: String
}

object ForUpdate {
    
    case object Simple extends ForUpdate {
        override def sqlSuffix: String = " FOR UPDATE"
    }
    
    case object NoWait extends ForUpdate {
        override def sqlSuffix: String = " FOR UPDATE NOWAIT"
    }
    
    case object SkipLocked extends ForUpdate {
        override def sqlSuffix: String = " FOR UPDATE SKIP LOCKED"
    }
    
    case class Of(columns: List[String]) extends ForUpdate {
        require(columns.nonEmpty, "At least one column required for FOR UPDATE OF")
        override def sqlSuffix: String = s" FOR UPDATE OF ${columns.mkString(", ")}"
    }
    
    case class OfNoWait(columns: List[String]) extends ForUpdate {
        require(columns.nonEmpty, "At least one column required for FOR UPDATE OF")
        override def sqlSuffix: String = s" FOR UPDATE OF ${columns.mkString(", ")} NOWAIT"
    }
    
    case class OfSkipLocked(columns: List[String]) extends ForUpdate {
        require(columns.nonEmpty, "At least one column required for FOR UPDATE OF")
        override def sqlSuffix: String = s" FOR UPDATE OF ${columns.mkString(", ")} SKIP LOCKED"
    }
    
    def simple(): ForUpdate = Simple
    
    def noWait(): ForUpdate = NoWait
    
    def skipLocked(): ForUpdate = SkipLocked
    
    def of(columns: java.util.List[String]): ForUpdate = Of(columns.asScala.toList)
    
    def ofNoWait(columns: java.util.List[String]): ForUpdate = OfNoWait(columns.asScala.toList)
    
    def ofSkipLocked(columns: java.util.List[String]): ForUpdate = OfSkipLocked(columns.asScala.toList)
    
    def of(column: String, more: String*): ForUpdate = Of((column +: more).toList)
    
    def ofNoWait(column: String, more: String*): ForUpdate = OfNoWait((column +: more).toList)
    
    def ofSkipLocked(column: String, more: String*): ForUpdate = OfSkipLocked((column +: more).toList)
}