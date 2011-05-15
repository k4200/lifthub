package net.lifthub {
package server.internalevent.response {

import net.liftweb.common._

trait InternalEventResponse

case class ResStart(result: Box[String]) extends InternalEventResponse
case class ResStop(result: Box[String]) extends InternalEventResponse
case class ResClean(result: Box[String]) extends InternalEventResponse


}
}
