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

  val unknownResponse = Failure("Unknown response.")
  val timeout = Failure("Timeout occurred. (" + TIMEOUT + ")")


  /**
   * Adds the given user to the system.
   * This also does initial setup like adding the admin ssh key
   * to the user's authorized keys.
   */
  def addUser(user: User): Box[Int] = {
    //Send a message to the remote server
    server !! (AddUser(user.id.is.asInstanceOf[Int], user.email.is,
                       user.password.plain),
               TIMEOUT) match {
      case Some(x) => x match {
        case ResAddUser(box) => box
        case _ => unknownResponse
      }
      case None => timeout
    }
  }

  def removeUser(user: User): Box[Int] = {
    server !! (RemoveUser(user.gitoriousUserId.is), TIMEOUT) match {
      case Some(x) => x match {
        case ResRemoveUser(box) => box
        case _ => unknownResponse
      }
      case None => timeout
    }
  }

  def addSshKey(user: User): Box[Int] = {
    //Send a message to the remote server
    server !! (AddSshKey(user.gitoriousUserId.is, user.sshKey.is), TIMEOUT) match {
      case Some(x) => x match {
        case ResAddSshKey(box) => box
        case y => println(y); unknownResponse
      }
      case None => timeout
    }
  }


  // def addAdminSshKey(user: User): Box[Int] = {
  //   import net.liftweb.util._
  //   // Get admin SSH key.
  //   //TODO ok here?
  //   val adminSshKeyPath = Props.get("git.path.admin.pubkey") openOr "/home/lifthub/.ssh/id_rsa.pub"
  //   val adminSshKey = scala.io.Source.fromFile(adminSshKeyPath).mkString

  //   //Send a message to the remote server
  //   server !! (AddSshKey(user.gitoriousUserId.is, adminSshKey), TIMEOUT) match {
  //     case Some(x) => x match {
  //       case ResAddSshKey(box) => box
  //       case y => println(y); unknownResponse
  //     }
  //     case None => timeout
  //   }
  // }

  def removeSshKey(user: User): Box[Int] = {
    //server !! (RemoveSshKey(user.gitoriousUserId.is), TIMEOUT) match {
    server !! (RemoveSshKey(user.gitoriousSshKeyId.is), TIMEOUT) match {
      case Some(x) => x match {
        case ResRemoveSshKey(box) => box
        case _ => unknownResponse
      }
      case None => timeout
    }
  }

  def addProject(user: User, project: Project): Box[Int] = {
    server !! (AddProject(user.gitoriousUserId.is, project.name), TIMEOUT) match {
      case Some(x) => x match {
        case ResAddProject(box) => box
        case y => println(y); unknownResponse
      }
      case None => timeout
    }
  }

  def removeProject(project: Project): Box[Int] = {
    server !! (RemoveProject(project.name.is), TIMEOUT) match {
      case Some(x) => x match {
        case ResRemoveProject(box) => box
        case y => println(y); unknownResponse
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
