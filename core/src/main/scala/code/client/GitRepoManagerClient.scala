package net.lifthub {
package client {

import akka.actor.Actor

import net.liftweb.common._

import net.lifthub.model.{User, Project}
import net.lifthub.common.event.gitrepo._
import net.lifthub.common.event.gitrepo.response._

object GitRepoManagerClient {
  import net.lifthub.common.ActorConfig
  val TIMEOUT = 20000 // in milliseconds

  def server = {
    ActorConfig("gitrepomanager").map { x =>
      val actor = Actor.remote.actorFor(x.name, x.host, x.port)
      actor.setTimeout(TIMEOUT)
      actor
    } get
  }

  val unknowResponse = Failure("Unknown response.")
  val timeout = Failure("Time out")


  def addUser(user: User): Box[Int] = {
    //Send a message to the remote server
    server !! (AddUser(user.id.is.asInstanceOf[Int], user.email.is,
                       user.password.plain),
               TIMEOUT) match {
      case Some(x) => x match {
        case ResAddUser(box) => box
        case _ => unknowResponse
      }
      case None => timeout
    }
  }

  def addSshKey(user: User): Box[Int] = {
    //Send a message to the remote server
    server !! (AddSshKey(user.gitoriousUserId.is, user.sshKey.is), TIMEOUT) match {
      case Some(x) => x match {
        case ResAddSshKey(box) => box
        case y => println(y); unknowResponse
      }
      case None => timeout
    }
  }

  def addProject(user: User, project: Project): Box[Int] = {
    server !! (AddProject(user.gitoriousUserId.is, project.name), TIMEOUT) match {
      case Some(x) => x match {
        case ResAddProject(box) => box
        case y => println(y); unknowResponse
      }
      case None => timeout
    }
  }

//TODO implement methods for the following messages.
// case class RemoveUser(@BeanProperty user: User) extends GitRepoEvent
// case class RemoveProject(@BeanProperty project: Project) extends GitRepoEvent
// case class RemoveSshKey(@BeanProperty user: User) extends GitRepoEvent

  /**
   * For testing.
   */
  def main(args: Array[String]) = {
    import net.liftweb.mapper._
    import bootstrap.liftweb.Boot
    
    val boot = new Boot
    boot.boot

    val user = User.find(1).get
    println("GitRepoManagerClient#main")
    addSshKey(user) match {
      case Full(x) => println(x)
      case Empty => println("Empty returned")
      case Failure(x, _, _) => println(x)
    }
  }
}

}
}
