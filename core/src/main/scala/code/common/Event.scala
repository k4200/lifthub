package net.lifthub {
package common.event.server {

//import scala.reflect.BeanInfo
//import akka.serialization.Serializable.ScalaJSON

import net.lifthub.lib.ServerInfo

/**
 * The parent of all the events that Server Manager handles.
 */
sealed trait ServerEvent

/**
 * Creates an environment.
 */
case class Create(projectName: String, port: Int) extends ServerEvent
/**
 * Stops the server of the project of the given ID.
 */
case class Delete(projectName: String) extends ServerEvent

/**
 * Starts the server of the project of the given ID.
 */
case class Start(projectName: String) extends ServerEvent
/**
 * Stops the server of the project of the given ID.
 */
case class Stop(projectName: String) extends ServerEvent


/**
 * Kills all the processes associated with the server
 * and cleans the runtime environment.
 * TODO Shouldn't this be here?
 */
case class Clean(projectName: String) extends ServerEvent





}
}
