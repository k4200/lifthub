package net.lifthub {
package client {

import akka.actor.Actor

import net.liftweb.common._

import net.lifthub.model.Project
import net.lifthub.common.event._

//class ServerManagerClient {
object ServerManagerClient {
  import net.lifthub.common.ActorConfig._
  val TIMEOUT = 60000

  def server = {
    val actor = Actor.remote.actorFor(REGISTER_NAME, SERVER_HOST, SERVER_PORT)
    actor.setTimeout(TIMEOUT)
    actor
  }

  def startServer(project: Project): Box[String] = {
    //Send a message to the remote server
    server !! (Start(project.id.is), TIMEOUT) match {
      case Some(x) if x == Response.STARTED  =>
        Full("successfully started! " + project.id)
      case _ => Failure("failed to start a server for " + project.id)
    }
  }

  def stopServer(project: Project): Box[String] = {
    server !! (Stop(project.id.is), TIMEOUT) match {
      case Some(x) if x == Response.STOPPED  =>
        Full("successfully stopped! " + project.id)
      case _ => Failure("failed to stop the server for " + project.id)
    }
  }

  def clean(project: Project): Box[String] = {
    server !! (Clean(project.id.is), TIMEOUT) match {
      case Some(x) if x == Response.CLEANED_UP  =>
        Full("successfully cleaned up! " + project.id)
      case _ => Failure("failed to clean the server environment for " + project.id)
    }
  }

  /**
   * For testing.
   */
  def main(args: Array[String]) = {
    import net.liftweb.mapper._
    import bootstrap.liftweb.Boot
    
    val boot = new Boot
    boot.boot

    val project = Project.find(By(Project.id, 1)).get
    println("ServerManagerClient#main")
    startServer(project) match {
      case Full(x) => println(x)
      case _ => println("failed")
    }
  }
}

}
}
