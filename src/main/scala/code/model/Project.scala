package net.lifthub {
package model {

import net.liftweb._
import common._
import mapper._
import http.S

object Project extends Project with LongKeyedMetaMapper[Project] {
  override def dbTableName = "projects"; // define the DB table name
//  override def fieldOrder = List(name, dateOfBirth, url)
}

class Project extends LongKeyedMapper[Project] with IdPK {
  def getSingleton = Project
  object name extends MappedString(this, 20) {
    override def validations = valUnique(S.??("unique.project.name")) _ :: super.validations
  }
  object user extends MappedLongForeignKey(this, User)
}


}
}
