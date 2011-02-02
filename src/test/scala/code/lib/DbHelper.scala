package net.lifthub {
package lib {


import net.liftweb.mapper.{DriverType,MySqlDriver}

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
    val boot = new Boot
    boot.boot
    //init

    "return the correct driver." in {
      MySqlHelper.getDriverType mustEqual MySqlDriver
    }
    "create a database." in {
      MySqlHelper.addDatabase("foo", DbUser("user","pass"))
      true mustBe true
    }
  }


}


}
}
