package net.lifthub {
package model {


import net.liftweb._
import common._
import mapper._
import http.S

import net.lifthub.model._
import net.lifthub.lib._


object UserDatabase extends UserDatabase with LongKeyedMetaMapper[UserDatabase]
with UserEditableCRUDify[Long, UserDatabase] {
  override def dbTableName = "user_database"; // define the DB table name
//  override def fieldOrder = List(name, dateOfBirth, url)

  import org.apache.commons.lang.RandomStringUtils
  def create(project: Project): UserDatabase = {
    val plainPassword = RandomStringUtils.randomAlphanumeric(8)
    //TODO MySQL
    val a = super.create.name(project.name).databaseType(DbType.MySql).username(project.name).password(plainPassword)
    a.plainPassword = Full(plainPassword)
    a
  }

  override def beforeSave = List(userDatabase =>  {
    //TODO instantiate one of DbHelper subclasses.
    //TODO create a database using DbHelper
  })
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

  /**
   * Hashed in the database.
   */
  object password extends MappedPassword(this) {
  }

  /**
   * Users shouldn't be able to change this value.
   */
  object hostname extends MappedString(this, 20) {
    override def defaultValue = "localhost" 
  }
  
  
  var plainPassword: Box[String] = Empty
}



}
}
