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
  "AggregateFunctions" should { doBefore { addRecords() }
    //TODO this appears in so many places.

    "provide max" in {
      Project.getAvailablePort mustEqual 9003
      Project.max(Project.port, By(Project.name, "foo")) mustEqual 9001
    }

    //TODO Write more test cases.

    def addRecords() = {
      val boot = new Boot
      boot.boot

      (for {
        _ <- tryo{DB.runUpdate("truncate table projects", Nil)} ?~ "truncte failed."
        _ <- tryo{DB.runUpdate("insert into projects (name,lift_version,userid,lift_template_type, port)values('foo','2.2',1,0, 9001)", Nil)} ?~ "insert1 failed."
        _ <- tryo{DB.runUpdate("insert into projects (name,lift_version,userid,lift_template_type, port)values('bar','2.2',1,0, 9002)", Nil)} ?~ "insert2 failed."
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
