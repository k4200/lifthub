package net.lifthub {
package model {

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._

import scala.io.Source

import org.specs._

import bootstrap.liftweb.Boot

import net.lifthub.lib.GitosisHelper

object ProjectTemplateSpec extends Specification {
  "ProjectTemplate" should { doBefore { addRecords() }
    "do something." in {
      true mustEqual true
    }

    def addRecords() = {
      val boot = new Boot
      boot.boot

      (for {
        _ <- tryo{DB.runUpdate("truncate table users", Nil)} ?~ "truncte failed."
        _ <- tryo{DB.runUpdate("insert into users (firstname,lastname,email,ssh_key)values('John','Doe','john@example.com','dummy-key')", Nil)} ?~ "insert1 failed."
        _ <- tryo{DB.runUpdate("insert into users (firstname,lastname,email)values('Taro','Yamada','taro@example.com')", Nil)} ?~ "insert2 failed."
      } yield {
        println("addRecords succeeded.")
      }) match {
        case ok: Full[_] => ok
        case ng => {
          println(ng)
        }
      }
    }
  }
}


}
}
