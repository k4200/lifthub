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
case class Create(serverInfo: ServerInfo) extends ServerEvent
/**
 * Stops the server of the project of the given ID.
 */
case class Delete(serverInfo: ServerInfo) extends ServerEvent

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




/**
 * Responses from ServerManager to client
 * @deprecated
 */
object Response {
  val STOPPED = "Stopped"
  val STARTED = "Started"
  val CLEANED_UP = "Cleaned up"
  val FAILED = "Failed"
}




}
}
