package net.lifthub {
package model {

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._

import scala.io.Source

import org.specs._

import bootstrap.liftweb.Boot

import net.lifthub.lib.GitosisHelper

object UserSpec extends Specification {
  "User" should { doBefore { addRecords() }
    "register a new ssh key." in {
      //init
      val user = User.find(By(User.id, 10)).get
      val keyFile = GitosisHelper.keyFile(user)
      keyFile.delete

      // key file doesn't exist.
      val curKey = getKeyString(keyFile)
      curKey mustEqual ""

      user.firstName("Jane")
      user.save
      val newKey1 = getKeyString(keyFile)
      // ssh-key hasn't been modified, so no file should be created.
      newKey1 mustEqual ""

      user.sshKey("new key")
      user.save
      val newKey2 = getKeyString(keyFile)
      newKey2 mustEqual "new key"
    }

    "return maxNumProjects" in {
      val user1 = User.find(By(User.id, 10)).get
      val user2 = User.find(By(User.id, 11)).get
      val user3 = User.find(By(User.id, 12)).get

      user1.maxNumProjects mustEqual 3
      user2.maxNumProjects mustEqual 1
      user3.maxNumProjects mustEqual 1
    }

    def getKeyString(keyFile: java.io.File): String = {
      if (keyFile.exists) {
        Source.fromFile(keyFile, "UTF-8").mkString
      } else {
        ""
      }
    }

    def addRecords() = {
      val boot = new Boot
      boot.boot

      (for {
        _ <- tryo{DB.runUpdate("truncate table user_config", Nil)} ?~ "truncte user_config failed."
        _ <- tryo{DB.runUpdate("truncate table users", Nil)} ?~ "truncte failed."
        _ <- tryo{DB.runUpdate("insert into users (id,firstname,lastname,email,ssh_key)values(10, 'John','Doe','john@example.com','dummy-key')", Nil)} ?~ "insert1 failed."
        _ <- tryo{DB.runUpdate("insert into users (id,firstname,lastname,email)values(11, 'Taro','Yamada','taro@example.com')", Nil)} ?~ "insert2 failed."
        _ <- tryo{DB.runUpdate("insert into users (id,firstname,lastname,email)values(12, 'Hanako','Yamada','hanako@example.com')", Nil)} ?~ "insert3 failed."
        _ <- tryo{DB.runUpdate("insert into user_config (name,value,user_c)values('limit.maxprojects','3',10)", Nil)} ?~ "insert user_config1 failed."
        _ <- tryo{DB.runUpdate("insert into user_config (name,value,user_c)values('limit.maxprojects','a',11)", Nil)} ?~ "insert user_config1 failed."
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
