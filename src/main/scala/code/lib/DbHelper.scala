package net.lifthub {
package lib {


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
  def addDatabase(database: UserDatabase): Box[AnyRef]
  def dropDatabase(database: UserDatabase): Box[AnyRef]

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

  def runUpdate(query: String): Box[Int] = {
    tryo { DB.runUpdate(query, Nil, connectionIdentifier) }
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

  def addDatabase(database: UserDatabase): Box[AnyRef] = {
    if(database.validate != Nil) {
      return Failure(S.??("error.dbhelper.invalid"))
    }
    //TODO ugly... don't know how to write this.
    (for {
      password <- database.plainPassword
      _ <- runUpdate("CREATE DATABASE " + database.name)
      _ <- runUpdate("CREATE USER '%s'@'%s' IDENTIFIED BY '%s'"
                     .format(database.username, database.hostname, password))
      _ <- runUpdate("GRANT ALL on %s.* to '%s'@'%s'"
                     .format(database.name, database.username, database.hostname))
    } yield "Database %s added successfully".format(database.name)) match {
      case ok: Full[_] => ok
      case ng => {
        tryo { dropDatabase(database) }
        ng
      }
    }
  }

  /**
   * @return Full(message) if success, otherwise Failure
   */
  def dropDatabase(database: UserDatabase): Box[AnyRef] = {
    if(database.validate != Nil) {
      return Failure(S.??("error.dbhelper.invalid"))
    }
    for {
      _ <- runUpdate("DROP DATABASE " + database.name)
      _ <- runUpdate("DROP USER '%s'@'%s'".format(database.username, database.hostname))
    } yield "drop database succeeded."
  }

}



}
}
