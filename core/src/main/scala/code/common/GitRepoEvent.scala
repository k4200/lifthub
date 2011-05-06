package net.lifthub {
package common.event.gitrepo {

import scala.reflect.BeanProperty
//import akka.serialization.Serializable.ScalaJSON

import net.lifthub.model._


// Abstraction for operations on git repositories.
// Clients don't care about what happens behind the scene.
sealed trait GitRepoEvent


// Messages
case class AddUser(@BeanProperty user: User) extends GitRepoEvent
case class RemoveUser(@BeanProperty user: User) extends GitRepoEvent
case class AddProject(@BeanProperty project: Project,
		      @BeanProperty user: User) extends GitRepoEvent
case class RemoveProject(@BeanProperty project: Project) extends GitRepoEvent
case class AddSshKey(@BeanProperty user: User) extends GitRepoEvent
case class RemoveSshKey(@BeanProperty user: User) extends GitRepoEvent




}
}
