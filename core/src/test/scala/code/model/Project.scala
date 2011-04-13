package net.lifthub {
package model {

import org.specs._

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._

import bootstrap.liftweb.Boot
import net.lifthub.lib._


object ProjectSpec extends Specification {
  //TODO move
  "AggregateFunctions" should {
    doBefore { Setup.addRecords() }

    "provide max" in {
      Project.getAvailablePort mustEqual 9003
      Project.max(Project.port, By(Project.name, "foo")) mustEqual 9001
    }
  }

  //TODO Write more test cases.

  "Project" should {
    doBefore { Setup.addRecords() }
    "not delete a database used by other projects" in {
      for(projfoo <- Project.find(By(Project.name, "foo"));
          projbar <- Project.find(By(Project.name, "bar")))
      yield {
	projbar.delete_!
	val res = UserDatabase.find(By(UserDatabase.name, "foo"))
	//res must haveClass(Full[UserDatabase])
	res must haveClass[Full[UserDatabase]]
      }
    }
  }


  object Setup {
    def addRecords() = {
      val boot = new Boot
      boot.boot

      (for {
        _ <- tryo{DB.runUpdate("truncate table project_templates", Nil)} ?~ "truncte pt failed."
        _ <- tryo{DB.runUpdate("insert into project_templates (id,name)values(1,'Template 1')", Nil)} ?~ "insert ud failed."

        _ <- tryo{DB.runUpdate("truncate table user_databases", Nil)} ?~ "truncte ud failed."
        _ <- tryo{DB.runUpdate("insert into user_databases (id,name,username)values(1,'testfoo','testfoo')", Nil)} ?~ "insert ud failed."

        _ <- tryo{DB.runUpdate("truncate table projects", Nil)} ?~ "truncte failed."
        _ <- tryo{DB.runUpdate("insert into projects (name,template,userid,database_c, port)values('foo',1,1,1, 9001)", Nil)} ?~ "insert1 failed."
        _ <- tryo{DB.runUpdate("insert into projects (name,template,userid,database_c, port)values('bar',1,1,1, 9002)", Nil)} ?~ "insert2 failed."
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
