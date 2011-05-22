package net.lifthub {
package client {

import akka.actor.Actor

import net.liftweb.common._

import net.lifthub.model.Project
import net.lifthub.common.event.server._
import net.lifthub.common.event.server.response._
import net.lifthub.lib.ServerInfo

object ServerManagerClient {
  import net.lifthub.common.ActorConfig
  val TIMEOUT = 60000

  def server = {
    ActorConfig("servermanager").map { x =>
      val actor = Actor.remote.actorFor(x.name, x.host, x.port)
      actor.setTimeout(TIMEOUT)
      actor
    } get
  }

  val unknownResponse = Failure("Unknown response.")

  // ---------------- actions
  def create(project: Project): Box[String] = {
    val serverInfo = ServerInfo(Project)
    server !! (Create(serverInfo), TIMEOUT) match {
      case Some(x) => x match {
        case ResCreate(box) => box
        case y => println(y); unknownResponse
      }
      case _ => Failure("failed to create an environment for project " + project.id)
    }
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
