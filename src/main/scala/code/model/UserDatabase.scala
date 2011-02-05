package net.lifthub {
package model {


import net.liftweb._
import common._
import mapper._
import http.S

import net.lifthub.model._
import net.lifthub.lib._


object DbType extends Enumeration {
  val MySql = DbTypeVal("MySQL")
  //val PostgreSql = DbTypeVal("PostgreSQL")

  case class DbTypeVal(name: String) extends Val
  implicit def valueToDbTypeValue(v: Value): DbTypeVal
    = v.asInstanceOf[DbTypeVal]
}

object UserDatabase extends UserDatabase with LongKeyedMetaMapper[UserDatabase]
with UserEditableCRUDify[Long, UserDatabase] {
  override def dbTableName = "database"; // define the DB table name
//  override def fieldOrder = List(name, dateOfBirth, url)

}

class UserDatabase extends LongKeyedMapper[UserDatabase] with IdPK
with UserEditableKeyedMapper[Long, UserDatabase] {
  def getSingleton = UserDatabase
  // Lifthub user, not a database one.
  override val userObject = User

  //object user extends MappedLongForeignKey(this, User)

  /**
   * The same as the project name.
   */
  object name extends MappedString(this, 20) {
    override def validations = valUnique(S.??("unique.database.name")) _ :: super.validations
  }

  object databaseType extends MappedEnum[UserDatabase, DbType.type](this, DbType) {
    override def dbColumnName = "database_type"
  }

  object username extends MappedString(this, 20) {
    //override def validations = valUnique(S.??("unique.database.name")) _ :: super.validations
  }

  object password extends MappedPassword(this) {
  }
}



}
}
