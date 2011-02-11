package net.lifthub {
package lib {


import net.liftweb.mapper.{DriverType,MySqlDriver}
import net.liftweb.common._

import org.specs._

import bootstrap.liftweb.Boot

import net.lifthub.model.UserDatabase


object DbHelperSpec extends Specification {

  //TODO move
  "DbType" should {
    "should provide attributes." in {
      DbType.MySql.name mustEqual "MySQL"
      DbType.MySql.driver mustEqual "com.mysql.jdbc.Driver"
    }
  }

  "MySqlHelper" should {
    shareVariables()
    // This makes the following code called only once, hopefully!
    val boot = new Boot
    boot.boot
    
    val db = UserDatabase.create.name("foo").username("foo").password("password")
    
    "return the correct driver." in {
      MySqlHelper.getDriverType mustEqual MySqlDriver
    }
    "create a database." in {
      MySqlHelper.addDatabase(db) must haveClass[Full[_]]
    }
    "fail to create a database with an invalid name." in {
      val invalidDb = UserDatabase.create.name("123").username("foo")
      MySqlHelper.addDatabase(invalidDb) must haveClass[Failure]
    }
    "drop a database." in {
      MySqlHelper.dropDatabase(db) must haveClass[Full[_]]
    }
  }


}


}
}
