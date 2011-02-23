package net.lifthub {
package client {

import akka.actor.Actor

import net.liftweb.common._

import net.lifthub.model.Project
import net.lifthub.common.event._

//class ServerManagerClient {
object ServerManagerClient {
  import net.lifthub.common.ActorConfig._

  def server = {
    val actor = Actor.remote.actorFor(REGISTER_NAME, SERVER_HOST, SERVER_PORT)
    actor.setTimeout(60000)
    actor
  }

  def startServer(project: Project): Box[String] = {
    //Send a message to the remote server
    server !! Start(project.id.is) match {
      case Some(x) if x == Response.STARTED  =>
        Full("successfully started! " + project.id)
      case _ => Failure("failed to start a server for " + project.id)
    }
  }

  def stopServer(project: Project): Box[String] = {
    server !! Stop(project.id.is) match {
      case Some(x) if x == Response.STOPPED  =>
        Full("successfully stopped! " + project.id)
      case _ => Failure("failed to stop the server for " + project.id)
    }
  }
}

}
}
