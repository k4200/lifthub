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
    //TODO for now
    val streamHandler = new PumpStreamHandler(null, null, null)
    executor.setWatchdog(watchdog)
    executor.setStreamHandler(streamHandler)
    executor.setWorkingDirectory(new File(server.basePath))

    try {
      // Don't wait for the process to finish.
      executor.execute(cmdLine)
      Full("command succeeded.")
    } catch {
      case e: ExecuteException =>
	val msg = "Failed to execute %s %s with exit code %d."
                   .format(COMMAND, args, e.getExitValue)
        Failure(msg, Full(e), Empty)
    }
  }

}

// object Test {
//   def main(args: Array[String]) = {
//     import net.liftweb.common._
//     import net.liftweb.mapper._
//     import net.lifthub.model.Project
//     import net.lifthub.lib.ServerInfo
//     import bootstrap.liftweb.Boot
    
//     val boot = new Boot
//     boot.boot

//     val project = Project.find(By(Project.id, 1)).get
//     val serverInfo = ServerInfo(project)
//     val args = List("start", serverInfo.projectName, serverInfo.stopPort.toString)
//     //val args2 = List("-DSTOP.PORT=10000", "-DSTOP.KEY=foo", "-jar", "start.jar", "etc/lifthub/foo.xml")
//     execute(serverInfo, args) match {
//       case Full(x) => println(x)
//       case Failure(x, _, _) => println(x)
//       case _ => println("error")
//     }
//   }

//   def execute(server: ServerInfo, args: List[String]) = {
//     val COMMAND = "bin/jetty-run-lifthub.sh"
//     //val COMMAND = "java"
//     val TIMEOUT = 20000
//     val cmdLine = new CommandLine(COMMAND)
//     args.foreach(cmdLine.addArgument _)

//     val executor = new DefaultExecutor
//     val watchdog = new ExecuteWatchdog(TIMEOUT)
//     val streamHandler = new PumpStreamHandler(null, null, null)
//     executor.setWatchdog(watchdog)
//     executor.setStreamHandler(streamHandler)
//     executor.setWorkingDirectory(new File(server.basePath))

//     try {
//       // Don't wait for the process to finish.
//       println("starting")
//       executor.execute(cmdLine)
//       Full("command succeeded.")
//     } catch {
//       case e: ExecuteException =>
// 	val msg = "Failed to execute %s %s with exit code %d."
//                    .format(COMMAND, args, e.getExitValue)
//         Failure(msg, Full(e), Empty)
//     }
//   }
// }

}
}
