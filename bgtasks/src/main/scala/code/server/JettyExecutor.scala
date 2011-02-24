package net.lifthub {
package server {

import java.io.File

import net.liftweb.common._

import akka.actor.Actor
import akka.actor.Actor._

import org.apache.commons.exec._

import internalevent._

import net.lifthub.lib.ServerInfo

class JettyExecutor extends Actor {
  val COMMAND = "bin/jetty-run-lifthub.sh"
  val TIMEOUT = 60000
  def receive = {
    case Start(serverInfo) =>
      val args = List("start", serverInfo.projectName, serverInfo.stopPort.toString)
      execute(serverInfo, args)
    case Stop(serverInfo) =>
      val args = List("stop", serverInfo.projectName, serverInfo.stopPort.toString)
      execute(serverInfo, args)
  }

  def execute(server: ServerInfo, args: List[String]) = {
    val cmdLine = new CommandLine(COMMAND)
    args.foreach(cmdLine.addArgument _)

    //val resultHandler = new DefaultExecuteResultHandler()

    val executor = new DefaultExecutor()
    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)
    executor.setWorkingDirectory(new File(server.basePath))

    try {
      // Don't wait for the process to finish.
      executor.execute(cmdLine)
      Full("jetty started.")
    } catch {
      case e: ExecuteException =>
	val msg = "Failed to execute %s %s with exit code %d."
                   .format(COMMAND, args, e.getExitValue)
        Failure(msg, Full(e), Empty)
    }
  }
}

}
}
