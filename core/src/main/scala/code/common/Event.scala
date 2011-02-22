package net.lifthub {
package common.event {

//import scala.reflect.BeanInfo
//import akka.serialization.Serializable.ScalaJSON

sealed trait Event

/**
 * Starts the server of the project of the given ID.
 */
case class Start(projectId: Long) extends Event
/**
 * Stops the server of the project of the given ID.
 */
case class Stop(projectId: Long) extends Event



/**
 * Responses from ServerManager to client
 */
object Response {
  val STOPPED = "Stopped"
  val STARTED = "Started"
  val FAILED = "Failed"
}




}
}
