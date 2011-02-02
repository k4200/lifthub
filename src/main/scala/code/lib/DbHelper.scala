package net.lifthub {
package lib {


//import net.liftweb.mapper.{DriverType,MySqlDriver, ConnectionIdentifier, DB}
import net.liftweb.mapper._

case class DbUser(_user: String, _password: String) {
  val replacePattern = "[^\\w\\d]".r
  val user = replacePattern.replaceAllIn(_user, "")
  val password = replacePattern.replaceAllIn(_password, "")
}


abstract class DbHelper[T <: DriverType](driverType: T) {
  def getDriverType(): T = driverType

  // Abstract methods
  def addDatabase(name: String, owner: DbUser): Unit

  val dbnamePattern = "^\\w[\\w\\d]*$".r

}

object MySqlHelper extends DbHelper(MySqlDriver) {
  //val connectionManager: ConnectionManager = new 
  //TODO
  val connectionIdentifier: ConnectionIdentifier = DefaultConnectionIdentifier
  override def addDatabase(name: String, owner: DbUser): Unit = {
    (for(safeName <- dbnamePattern.findFirstIn(name))
    yield {
      tryo {
        DB.runUpdate("create database " + safeName,
          Nil,connectionIdentifier)
      } openOr {
	println("couldn't create a database " + safeName)
      }
      //DB.performQuery("create database " + safeName)
    }) getOrElse {
      //Error
      println("DB name is invalid: " + name)
    }
  }

}



}
}
