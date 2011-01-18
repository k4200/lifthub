package net.lifthub {
package model {

import net.liftweb._
import common._
import mapper._


object Project extends Project with LongKeyedMetaMapper[Project] {
  override def dbTableName = "projects"; // define the DB table name
//  override def fieldOrder = List(name, dateOfBirth, url)
}

class Project extends LongKeyedMapper[Project] with IdPK {
  def getSingleton = Project
  object name extends MappedString(this, 20)
  object user extends MappedLongForeignKey(this, User)
}


}
}
