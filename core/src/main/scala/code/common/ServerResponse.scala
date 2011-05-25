package net.lifthub {
package common.event.server.response {

import net.liftweb.common.Box


trait ServerResponse


case class ResStart(result: Box[String]) extends ServerResponse
case class ResStop(result: Box[String]) extends ServerResponse
case class ResClean(result: Box[String]) extends ServerResponse

case class ResCreate(result: Box[String]) extends ServerResponse
case class ResDelete(result: Box[String]) extends ServerResponse



}
}
