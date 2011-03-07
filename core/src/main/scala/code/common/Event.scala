package net.lifthub {
package common.event {

//import scala.reflect.BeanInfo
//import akka.serialization.Serializable.ScalaJSON

/**
 * The parent of all the events that Server Manager handles.
 */
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
 * Kills all the processes associated with the server
 * and cleans the runtime environment.
 * TODO Shouldn't this be here?
 */
case class Clean(projectId: Long) extends Event



/**
 * Responses from ServerManager to client
 */
object Response {
  val STOPPED = "Stopped"
  val STARTED = "Started"
  val CLEANED_UP = "Cleaned up"
  val FAILED = "Failed"
}




}
}
