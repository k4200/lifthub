package net.lifthub {
package model {

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._

import scala.io.Source

import org.specs._

import bootstrap.liftweb.Boot

object UserConfigSpec extends Specification {
  "getValue" should {
    doBefore {
      addRecords()
    }
    "return a String" in {
      val user = User.find(By(User.id, 11)).get
      val box = UserConfig.getValue(user, "limit.maxprojects")
      //box must haveClass[Full[_]]
      box.getOrElse("") mustEqual "4"
    }

    "return an Empty when no value is found" in {
      val user = User.find(By(User.id, 11)).get
      val box = UserConfig.getValue(user, "limit.nonexistent")
      //box must haveSuperClass[EmptyBox]
      box mustBe Empty
    }

    def addRecords() = {
      val boot = new Boot
      boot.boot

      (for {
        _ <- tryo{DB.runUpdate("truncate table user_config", Nil)} ?~ "truncte user_config failed."
        _ <- tryo{DB.runUpdate("truncate table users", Nil)} ?~ "truncte users failed."
        _ <- tryo{DB.runUpdate("insert into users (id,firstname,lastname,email)values(10,'Taro','Yamada','taro@example.com')", Nil)} ?~ "insert user 10 failed."
        _ <- tryo{DB.runUpdate("insert into users (id,firstname,lastname,email)values(11,'Hanako','Yamada','hanako@example.com')", Nil)} ?~ "insert user 11 failed."
        _ <- tryo{DB.runUpdate("insert into user_config (name,value,user_c)values('limit.maxprojects','3',10)", Nil)} ?~ "insert user_config1 failed."
        _ <- tryo{DB.runUpdate("insert into user_config (name,value,user_c)values('limit.maxprojects','4',11)", Nil)} ?~ "insert user_config2 failed."
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
