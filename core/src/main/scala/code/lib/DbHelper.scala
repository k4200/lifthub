package net.lifthub {
package lib {

import scala.util.control.Exception._

//import net.liftweb.mapper.{DriverType,MySqlDriver, ConnectionIdentifier, DB}
import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.http.S
import net.liftweb.util._
import Helpers._

import scala.collection.mutable._

import net.lifthub.model.UserDatabase

object DbType extends Enumeration {
  val MySql = DbTypeVal("MySQL", "com.mysql.jdbc.Driver")
  //val PostgreSql = DbTypeVal("PostgreSQL")

  case class DbTypeVal(name: String, driver: String) extends Val
  implicit def valueToDbTypeValue(v: Value): DbTypeVal
    = v.asInstanceOf[DbTypeVal]
}


abstract class DbHelper[T <: DriverType](driverType: T) {
  def getDriverType(): T = driverType

  // Abstract methods
  def addDatabase(database: UserDatabase): Box[String]
  def dropDatabase(database: UserDatabase): Box[String]

  val dbnamePattern = "^\\w[\\w\\d]*$".r
  /**
   *
   */
  def vendor(): Box[ProtoDBVendor] = {
    val dbName = driverType.name.toLowerCase
    //val dbName = driverType.name //This will fail
    tryo {
      new StandardDBVendor(
        Props.get("userdb.%s.driver".format(dbName)) get,
        Props.get("userdb.%s.url".format(dbName)) get,
        Props.get("userdb.%s.user".format(dbName)),
        Props.get("userdb.%s.password".format(dbName)))
    }
  }

  def runUpdate(query: String): Int = {
    println(query) //Debug
    DB.runUpdate(query, Nil, connectionIdentifier)
  }

  def connectionIdentifier: ConnectionIdentifier
}

object DbHelper {
  val dbHelpers = new HashMap[DbType.Value, DbHelper[DriverType]]
  def addDbHelper(dbType: DbType.Value, dbHelper: DbHelper[DriverType]): Unit = {
    dbHelpers(dbType) = dbHelper
  }

  def all = {
    dbHelpers.values
  }

  def get(dbType: DbType.Value): DbHelper[DriverType] = {
    dbHelpers(dbType)
  }
}

object MySqlHelper extends DbHelper[DriverType](MySqlDriver) {
  object UserDbMySqlIdentifier extends ConnectionIdentifier {
    def jndiName = "userdb/mysql"
  }
  override def connectionIdentifier = UserDbMySqlIdentifier

  def addDatabase(database: UserDatabase): Box[String] = {
    if(database.validate != Nil) {
      return Failure(S.??("error.dbhelper.invalid"))
    }
    database.plainPassword match {
      case Full(password) =>
        allCatch either {
          runUpdate("CREATE DATABASE " + database.name)
          //TODO it would be better to limit the source host, but too much hassle.
          runUpdate("CREATE USER '%s'@'%%' IDENTIFIED BY '%s'"
                     .format(database.username, password))
          runUpdate("GRANT ALL on %s.* to '%s'@'%s'"
                     .format(database.name, database.username, database.hostname))
        } match {
          case Right(_) =>
            Full("Database %s added successfully".format(database.name))
          case Left(t) =>
            t.printStackTrace
            tryo { dropDatabase(database) }
            Failure("Failed to add database %s.".format(database.name),
                    Full(t), Empty)
        }
      case _ => Failure("password is not set.")
    }
  }

  /**
   * @return Full(message) if success, otherwise Failure
   */
  def dropDatabase(database: UserDatabase): Box[String] = {
    if(database.validate != Nil) {
      return Failure(S.??("error.dbhelper.invalid"))
    }
    tryo {
      runUpdate("DROP DATABASE " + database.name)
      runUpdate("DROP USER '%s'@'%%'".format(database.username))
      "Succeeded to drop database %s.".format(database.name)
    }
  }

}



}
}
