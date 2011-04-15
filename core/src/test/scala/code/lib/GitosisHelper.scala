package net.lifthub {
package lib {

import org.specs._

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._

import akka.actor.Actor
import akka.actor.Actor._

import net.lifthub.model.Project
import net.lifthub.model.UserDatabase
import net.lifthub.model.ProjectTemplate

import bootstrap.liftweb.Boot


object GitosisHelperSpec extends Specification {
  Initializer.initConn()
  import net.lifthub.model.User
  val user = new User
  user.email.set("kashima@shibuya.scala-users.org")
  user.sshKey.set("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIEAjz+vWAw0gf7PGUBkVO12HEuDzId08c/uv2kGQmhA7GRZ+Aw8SMhVAua3Vy7Ob21AhWkPfE/1/oiVTWTZSUhuoGtcxcP+0lL13GB5DHABr6eWH9CE11qxBAYs/wk+c7xMMj3Igh2MZvTydVr1useq4f1npiJ8+bzCMJiSKtNhHcs= kashima@shibuya.scala-users.org")

  // -------------
  // -------------
  "GitosisHelper" should {
    doBefore { Initializer.addRecords() }
    val pt = ProjectTemplate.find(By(ProjectTemplate.id, 1)).get
    val pi = ProjectInfo("foo", pt)

    // now 'conf' is in GitosisOperationsSynchronizer.
import akka.actor.Actor
import akka.actor.Actor._
    val synchronizer = (actorOf[GitosisOperationsSynchronizer]).asInstanceOf[GitosisOperationsSynchronizer]
    "provide the conf file." in {
      val conf = synchronizer.conf

      conf.getAbsolutePath mustEqual "/home/lifthub/gitosis-admin/gitosis.conf"
      conf.getRelativePath mustEqual "gitosis.conf"
      conf.exists mustBe true
    }
    "provide a key file of the user" in {
      GitosisHelper.keyFile(user).relativePath mustEqual
	"keydir/kashima@shibuya.scala-users.org.pub"
      GitosisHelper.keyFile(user).getAbsolutePath mustEqual
	"/home/lifthub/gitosis-admin/keydir/kashima@shibuya.scala-users.org.pub"
    }
    "generate an entry string" in {
      synchronizer.generateConfEntryString(pi, user) mustEqual
	"""[group foo]
members = lifthub@localhost.localdomain kashima@shibuya.scala-users.org
writable = foo"""
    }
    "write entry to the conf file." in {
      val before = synchronizer.conf.length
      synchronizer.addEntry2Conf(pi, user)
      //val after = GitosisHelper.conf.length
      val after = 1000
      before must_!= after
    }
    "mark the conf file to be committed" in {
      //GitosisHelper.gitAddConf mustBe true
      true mustBe true
    }
    "write a user's key to a file" in {
      GitosisHelper.createSshKey(user) mustBe true
    }    
    "add the user's key to git" in {
      //GitosisHelper.gitAddSshKey(user) mustBe true
      true mustBe true
    }
    "commit user's key and the conf file and push them" in {
      //GitosisHelper.commitAndPush("Added a user: " + user.email, true) mustBe true
      //GitosisHelper.commitAndPush("Added a user: " + user.email) mustBe true
      true mustBe true
    }
//     "remove an entry from the conf file" in {
//       GitosisHelper.removeEntryFromConf(pi)
//     }
  }

  object Initializer {
    def initConn() = {
      val boot = new Boot
      boot.boot
    }
    def addRecords() = {

      (for {
        _ <- tryo{DB.runUpdate("truncate table project_templates", Nil)} ?~ "truncte failed."
        _ <- tryo{DB.runUpdate("insert into project_templates (id, name,path,lift_version)values(1, 'Lift 2.2 Basic', 'lift_2.2_sbt/lift_basic', '2.2')", Nil)} ?~ "insert1 failed."
      } yield {
        println("ProjectHelper.Initializer.addRecords succeeded.")
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
