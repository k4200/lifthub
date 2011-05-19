package net.lifthub {
package model {

import org.specs._
import org.specs.mock.Mockito
import org.mockito.Matchers._  // to use matchers like anyInt()

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._

import bootstrap.liftweb.Boot
import net.lifthub.lib._


object ProjectSpec extends Specification with Mockito {
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
	val res = UserDatabase.find(By(UserDatabase.name, "testfoo"))
	//res must haveClass(Full[UserDatabase])
	res must haveClass[Full[UserDatabase]]
      }
    }
    // "delete the database when deleting a project" in {
    //   for(projfoo <- Project.find(By(Project.name, "foo")))
    //   yield {
    //     //set up a mock
    //     val m = mock[UserDatabase]

    //     m.id.is returns 1
    //     m.name.is returns "testfoo"
    //     m.username.is returns "testfoo"
    //     m.dropDatabase returns Full("succeeded to drop foo.")

    //     projfoo.database(m)
    //     projfoo.delete_!
    //     there was one(m).dropDatabase
    //   }
    // }

    // "copy template after creating a project" in {
    // }


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
