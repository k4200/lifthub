package net.lifthub {
package lib {


//import net.liftweb.mapper.{DriverType,MySqlDriver, ConnectionIdentifier, DB}
import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.util._
import Helpers._


case class DbUser(_user: String, _password: String) {
  val replacePattern = "[^\\w\\d]".r
  val user = replacePattern.replaceAllIn(_user, "")
  val password = replacePattern.replaceAllIn(_password, "")
}


abstract class DbHelper[T <: DriverType](driverType: T) {
  def getDriverType(): T = driverType

  // Abstract methods
  def addDatabase(name: String, owner: DbUser, host: String): Box[AnyRef]
  def dropDatabase(name: String, owner: DbUser, host: String): Box[AnyRef]

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

  def checkDbName(name: String): Box[String] = {
    dbnamePattern.findFirstIn(name) match {
      case Some(safeName) => Full(safeName)
      case _ => Failure("DB name is invalid:" + name)
    }
  }
}

object MySqlHelper extends DbHelper(MySqlDriver) {
  object UserDbMySqlIdentifier extends ConnectionIdentifier {
    def jndiName = "userdb/mysql"
  }
  override def connectionIdentifier = UserDbMySqlIdentifier

  def addDatabase(name: String, owner: DbUser, host: String = "localhost") = {
    //TODO ugly... don't know how to write this.
    (for {
      safeName <- checkDbName(name)
      _ <- runUpdate("CREATE DATABASE " + safeName)
      _ <- runUpdate("CREATE USER '%s'@'%s' IDENTIFIED BY '%s'"
                    .format(owner.user, host, owner.password))
      _ <- runUpdate("GRANT ALL on %s.* to '%s'@'%s'"
                  .format(safeName, owner.user, host))
    } yield "Database %s added successfully".format(safeName)) match {
      case ok: Full[_] => ok
      case ng => {
	tryo { dropDatabase(name, owner, host) }
	ng
      }
    }
  }

  /**
   * @return Full(message) if success, otherwise Failure
   */
  def dropDatabase(name: String, owner: DbUser, host: String = "localhost"): Box[String] = {
    for {
      safeName <- checkDbName(name)
      _ <- runUpdate("DROP DATABASE " + safeName)
      _ <- runUpdate("DROP USER '%s'@'%s'".format(owner.user, host))
    } yield "drop database succeeded."
  }

}



}
}
