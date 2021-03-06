package net.lifthub {
package model {


import net.liftweb._
import util._
//import util.{FieldError,FieldIdentifier,StringValidators}
import common._
import mapper._
import http.S

import scala.xml.Text

import net.lifthub.model._
import net.lifthub.lib._


object UserDatabase extends UserDatabase with LongKeyedMetaMapper[UserDatabase]
with UserEditableCRUDify[Long, UserDatabase] {
  override def dbTableName = "user_databases"; // define the DB table name
//  override def fieldOrder = List(name, dateOfBirth, url)

  val DEFAULT_DBTYPE = DbType.MySql

  import org.apache.commons.lang.RandomStringUtils
  def createFromProject(project: Project): UserDatabase = {
    val plainPassword = RandomStringUtils.randomAlphanumeric(8)
    val dbType = DEFAULT_DBTYPE
    val hostname = Props.get("userdb.mysql.hostname.internal") openOr "localhost"
    create.name(project.name).databaseType(dbType).hostname(hostname)
    .username(project.name).password(plainPassword)
  }

  // def createFromName(name: String): UserDatabase = {
  //   val plainPassword = RandomStringUtils.randomAlphanumeric(8)
  //   val dbType = DEFAULT_DBTYPE
  //   create.name(name).databaseType(dbType).username(name).password(plainPassword)
  // }

  override def beforeCreate = List(userDatabase =>  {
    //TODO ugly... 
    userDatabase.addDatabase match {
      case Failure(msg, box, _) =>
        box.map(t => throw t)
      case _ =>
        //
    }
  })

  override def afterDelete = List(userDatabase =>  {
    userDatabase.dropDatabase
  })

  override def fieldsForEditing = List(password)

  // Disable the Edit menu
  override def createMenuLoc = Empty
  override def editMenuLoc = Empty
}



class UserDatabase extends LongKeyedMapper[UserDatabase] with IdPK
with UserEditableKeyedMapper[Long, UserDatabase] {
  def getSingleton = UserDatabase
  // Lifthub user, not a database one.
  override val userObject = User

  //object user extends MappedLongForeignKey(this, User)

  val namePattern = "^[a-zA-Z]\\w*$".r.pattern
  val passPattern = "^\\w+$".r.pattern
  /**
   * The same as the project name.
   */
  object name extends MappedString(this, 20) {
    override def validations =
      valUnique(S.??("validator.database.name.unique")) _ ::
      valRegex(namePattern, S.??("validator.database.name.invalid")) _ ::
      super.validations
  }

  object databaseType extends MappedEnum[UserDatabase, DbType.type](this, DbType) {
    override def dbColumnName = "database_type"
  }

  object username extends MappedString(this, 20) {
    override def validations =
      valUnique(S.??("validator.database.username.unique")) _ ::
      valRegex(namePattern, S.??("validator.database.username.invalid")) _ ::
      super.validations
  }

  /**
   * Hashed in the database.
   */
  object password extends MappedPassword(this) with StringValidators {
    override def set(value: String): String = {
      plainPassword = value
      super.set(value)
    }
    
    /**
     * MappedPassword redifines <code>validate</code> and ignores
     * <code>validations.</code>
     */
    override def validate: List[FieldError] = {
      (plainPassword match {
        case Full(str) =>
          if (passPattern.matcher(str).matches) Nil
          else List(FieldError(this, Text(S.??("validator.database.password.invalid"))))
        case _ => Nil
      }) ::: (super.validate)
    }

    def valueTypeToBoxString(in: String): Box[String] = Full(in)
    def boxStrToValType(in: Box[String]): String = in openOr ""
    def maxLen = 20 //TODO ok?
  }

  /**
   * Users shouldn't be able to change this value.
   */
  object hostname extends MappedString(this, 20) {
    override def defaultValue = "localhost" 
  }
  
  
  var _plainPassword: Box[String] = Empty
  protected def plainPassword_=(value: String) = _plainPassword = Full(value)
  def plainPassword = _plainPassword

  //operations
  def addDatabase: Box[String] = {
    val dbHelper = DbHelper.get(databaseType)
    dbHelper.addDatabase(this)
  }

  def dropDatabase: Box[String] = {
    val dbHelper = DbHelper.get(databaseType)
    dbHelper.dropDatabase(this)
  }
}



}
}
