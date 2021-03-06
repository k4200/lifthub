package net.lifthub {
package model {

import net.liftweb.common._
import net.liftweb.mapper._
import scala.util.control.Exception._

import org.specs._
// import org.specs.mock.Mockito
// import org.mockito.Matchers._  // to use matchers like anyInt()

import bootstrap.liftweb.Boot

object UserDatabaseSpec extends Specification {


  "UserDatabase" should {
    shareVariables()
    val boot = new Boot
    boot.boot

    "check attributes." in {
      val db = UserDatabase.create
      db.name("foo")
      db.name.validate must equalTo(Nil)
      db.username("foo")
      db.username.validate must equalTo(Nil)
      db.password("password")
      db.password.validate must equalTo(Nil)

      db.name("1foo")
      db.name.validate.size must equalTo(1)
      db.username("1foo")
      db.username.validate.size must equalTo(1)
      db.password("1234abc!;")
      db.password.validate.size must equalTo(1)
    }

    "validate" in {
      val db = UserDatabase.create.name("foo").username("foo").password("1p2a3s4s")
      db.validate must equalTo(Nil)
    }

    "get a plain password." in {
      val pass = "f00bAR"
      val db = UserDatabase.create.password(pass)
      db.plainPassword match {
        case Full(str) => str mustEqual pass
        case _ => //fail
      }
    }

    "create a database" in {
      val db = UserDatabase.create.name("foo").username("foo").password("1p2a3s4s")
      db.addDatabase must haveClass[Full[String]]
    }

    "delete a database" in {
      val db = UserDatabase.find(1).get
      db.dropDatabase
    }

  }


  object Setup {
    def addRecords() = {
      val boot = new Boot
      boot.boot

      allCatch either {
        DB.runUpdate("truncate table user_databases", Nil)
      } match {
        case Right(_) =>
          //OK
        case Left(x) => 
          println(x)
      }
    }
  }


}


}
}


