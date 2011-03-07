package net.lifthub {
package server.internalevent {

import net.lifthub.lib.ServerInfo

sealed trait InternalEvent

/**
 * Starts the server of the project of the given ID.
 */
case class Start(serverInfo: ServerInfo) extends InternalEvent
/**
 * Stops the server of the project of the given ID.
 */
case class Stop(serverInfo: ServerInfo) extends InternalEvent

/**
 * Kills all the processes of the server and cleans up the runtime
 * environment
 */
case class Clean(serverInfo: ServerInfo) extends InternalEvent



}
}
