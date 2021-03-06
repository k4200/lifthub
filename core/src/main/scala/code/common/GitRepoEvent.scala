package net.lifthub {
package common.event.gitrepo {

//import scala.reflect.BeanProperty

import akka.serialization.Serializable.ScalaJSON

import net.lifthub.model._


// Abstraction for operations on git repositories.
// Clients don't care about what happens behind the scene.
sealed trait GitRepoEvent


// Messages
/**
 * userId is used only for user name.
 */
case class AddUser(userId: Int, email: String, password: String)
     extends GitRepoEvent
case class RemoveUser(gitoriousUserId: Int) extends GitRepoEvent
case class AddProject(gitoriousUserId: Int, projectName: String)
     extends GitRepoEvent
//Removing a project by name is useful in case the script that
// creates a project failed, but actually the project was created,
// in which case, we don't know the id.
case class RemoveProject(projectName: String) extends GitRepoEvent
case class AddSshKey(gitoriousUserId: Int, sshKey: String) extends GitRepoEvent
case class RemoveSshKey(gitoriousUserId: Int) extends GitRepoEvent




}
}
