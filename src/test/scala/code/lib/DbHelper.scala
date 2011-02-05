package net.lifthub {
package lib {


import net.liftweb.mapper.{DriverType,MySqlDriver}
import net.liftweb.common._

import org.specs._

import bootstrap.liftweb.Boot

object DbHelperSpec extends Specification {
  "DbUser" should {
    val dbUser = DbUser("a;b", "c;d")
    "contain input username and password" in {
      dbUser._user mustEqual "a;b"
      dbUser._password mustEqual "c;d"
    }
    "remove special chars in username and password" in {
      dbUser.user mustEqual "ab"
      dbUser.password mustEqual "cd"
    }
  }

  "MySqlHelper" should {
    shareVariables()
    // This makes the following code called only once, hopefully!
    val boot = new Boot
    boot.boot

    "return the correct driver." in {
      MySqlHelper.getDriverType mustEqual MySqlDriver
    }
    "create a database." in {
      MySqlHelper.addDatabase("lh_foo", DbUser("foo","pass")) must haveClass[Full[_]]
    }
    "fail to create a database with an invalid name." in {
      MySqlHelper.addDatabase("", DbUser("foo","pass")) must haveClass[Failure]
    }
    "drop a database." in {
      MySqlHelper.dropDatabase("lh_foo", DbUser("foo","pass")) must haveClass[Full[_]]
    }
  }


}


}
}
