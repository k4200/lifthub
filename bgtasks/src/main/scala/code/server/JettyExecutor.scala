package net.lifthub {
package server {

import java.io.File
import java.io.FileOutputStream

import net.liftweb.common._
import net.liftweb.util.Helpers._

import akka.actor.Actor
import akka.actor.Actor._

import org.apache.commons.exec._

import internalevent._

import net.lifthub.lib.ServerInfo

class JettyExecutor extends Actor {
  // This script uses chroot and calls the other script.
  //val COMMAND = "jetty-run-lifthub-root.sh"
  val COMMAND = "/home/lifthubuser/sbin/jetty-run-lifthub-root.sh"

  val TIMEOUT = 30000
  val KEYWORD_SUCCESS = "INFO::Started"
  val KEYWORD_FAILURE = "Exception"

  /**
   * Replies a Box[String].
   * This method blocks.
   */
  def receive = {
    case Start(serverInfo) =>
      val cmd = List("sudo", COMMAND, "start", serverInfo.projectName, serverInfo.stopPort.toString)
      self.reply(tryo {
	killAll(serverInfo)
        execute(serverInfo, cmd)
        checkProcess(serverInfo)
      })
    case Stop(serverInfo) =>
      val cmd = List(COMMAND, "stop", serverInfo.projectName, serverInfo.stopPort.toString)
      self.reply(tryo {
        execute(serverInfo, cmd)
	Full("stopped")
      })
    case Clean(serverInfo) =>
      val cmd = List("sudo", COMMAND, "clean", server.projectName)
      self.reply(tryo {
        execute(serverInfo, cmd)
  	Full("cleand up")
      })
  }

  /**
   * TODO Implement this.
   */
  def kill(server: ServerInfo) = {
    val args = List("kill", server.projectName)
  }

  def execute(server: ServerInfo, cmd: List[String]) = {
    val cmdLine = new CommandLine(cmd.head)
    cmd.tail.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)

    // Discard the output. (Actually, there's no output from the process
    // because the shell spript redirects it to the log file.)
    val streamHandler = new PumpStreamHandler(null, null, null)
    executor.setStreamHandler(streamHandler)

    val resultHandler = new DefaultExecuteResultHandler()
    executor.execute(cmdLine)  // synchronous

    println("JettyExecutor.execute finished.")
  }

  /**
   * Checks if the server has been started correctly.
   */
  def checkProcess(serverInfo: ServerInfo): Box[String] = {
    def parseLog(serverInfo: ServerInfo): Box[Boolean] = {
      val log = scala.io.Source.fromFile(serverInfo.executeLogPath).mkString
      if (log.contains(KEYWORD_FAILURE)) {
	Full(false)
      } else if (log.contains(KEYWORD_SUCCESS)) {
	Full(true)
      } else {
	Empty
      }
    }

    val start = System.currentTimeMillis
    while (true) {
      if (System.currentTimeMillis - start > TIMEOUT) {
	//timeout occured.
	kill(serverInfo)
        return  Failure("Timeout.")
      }
      parseLog(serverInfo) match {
        case Full(true) =>
          return  Full("Server started.")
        case Full(false) =>
          kill(serverInfo)
          return  Failure("An exception occured.")
        case Empty =>
          println("still running")
        case _ =>
          return  Failure("Unknown error.")
      }
      Thread.sleep(1000) //TODO
    }
    return Failure("Unknown error")
  }

  /**
   * Kills all the remaining processes associated with this server.
   */
  def killAll(serverInfo: ServerInfo) = {
    //TODO Implement this.
    // For now, kill the process of the pid in the pid file. 
    kill(serverInfo)
  }

}

object Test {
  val COMMAND = "bin/jetty-run-lifthub.sh"
  val TIMEOUT = 20000
  val KEYWORD_SUCCESS = "INFO::Started"
  val KEYWORD_FAILURE = "Exception"

  def main(args: Array[String]): Unit = {
    import net.liftweb.common._
    import net.liftweb.mapper._
    import net.lifthub.model.Project
    import net.lifthub.lib.ServerInfo
    import bootstrap.liftweb.Boot
    
    val boot = new Boot
    boot.boot

    val project = Project.find(By(Project.id, 1)).get
    val serverInfo = ServerInfo(project)
    val args = List("start", serverInfo.projectName, serverInfo.stopPort.toString)
    execute(serverInfo, args)

    val start = System.currentTimeMillis
    while (true) {
      if (System.currentTimeMillis - start > TIMEOUT) {
	//timeout occured.
	return
      }
      checkProcess(serverInfo) match {
        case Full(x) =>
          println("result = " + x)
          return 
        case Empty =>
          println("still running")
        case _ =>
          println("This shouldn't happen.")
      }
      Thread.sleep(1000)
    }

  }

  def execute(server: ServerInfo, args: List[String]) = {
    import java.io._
    val cmdLine = new CommandLine(COMMAND)
    args.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor
    executor.setWorkingDirectory(new File(server.basePath))

    val streamHandler = new PumpStreamHandler(null, null, null)
    executor.setStreamHandler(streamHandler)

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)

    executor.execute(cmdLine)
  }

  def checkProcess(serverInfo: ServerInfo): Box[Boolean] = {
    val log = scala.io.Source.fromFile(serverInfo.executeLogPath).mkString
    if (log.contains(KEYWORD_FAILURE)) {
      Full(false)
    } else if (log.contains(KEYWORD_SUCCESS)) {
      Full(true)
    } else {
      Empty
    }
  }

}

}
}
