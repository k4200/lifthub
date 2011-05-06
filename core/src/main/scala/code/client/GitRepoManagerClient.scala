package net.lifthub {
package client {

import akka.actor.Actor

import net.liftweb.common._

import net.lifthub.model.User
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


  def addUser(user: User): Box[Int] = {
    //Send a message to the remote server
    server !! (AddUser(user), TIMEOUT) match {
      case Some(x) => x match {
        case UserAdded(box) => box
        case f: Failure => f
        case _ => unknowResponse
      }
      case _ => Failure("failed to add a user " + user.id)
    }
  }

  def addSshKey(user: User): Box[Int] = {
    //Send a message to the remote server
    server !! (AddSshKey(user), TIMEOUT) match {
      case Some(x) => x match {
        case SshKeyAdded(box) => box
        case f: Failure => f
        case _ => unknowResponse
      }
      case _ => Failure("failed to add an ssh key for user " + user.id)
    }
  }

//TODO implement methods for the following messages.
// case class RemoveUser(@BeanProperty user: User) extends GitRepoEvent
// case class AddProject(@BeanProperty project: Project,
// 		      @BeanProperty user: User) extends GitRepoEvent
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
      case _ => println("failed")
    }
  }
}

}
}
