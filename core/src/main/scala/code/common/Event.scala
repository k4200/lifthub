package net.lifthub {
package common.event {

//import scala.reflect.BeanInfo
//import akka.serialization.Serializable.ScalaJSON

sealed trait Event

//@BeanInfo
case class Start(projectId: Long) extends Event
//with Serializable.ScalaJSON[Start]

//@BeanInfo
case class Stop(projectId: Long) extends Event
//with Serializable.ScalaJSON[Stop]


//@BeanInfo
case class StopServer() extends Event
//with Serializable.ScalaJSON[StopServer]


object Response {
  val STOPPED = "Stopped"
  val STARTED = "Started"
}




}
}
