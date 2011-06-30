package net.lifthub {
package server.internalevent {

import net.lifthub.lib.ServerInfo

/**
 * Events sent from ServerManager to Executor (JettyExecutor etc.)
 */
sealed trait InternalEvent

/**
 * Starts the server of the project of the given ID.
 */
case class Start(projectName: String, stopPort: Int) extends InternalEvent
/**
 * Stops the server of the project of the given ID.
 */
case class Stop(projectName: String, stopPort: Int) extends InternalEvent

/**
 * Kills all the processes of the server and cleans up the runtime
 * environment
 */
case class Clean(projectName: String) extends InternalEvent



}
}
