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
import internalevent.response._

import net.lifthub.lib.ServerInfo

class JettyExecutor extends Actor {
  // This script uses chroot and calls the other script.
  //val COMMAND = "jetty-run-lifthub-root.sh"
  val COMMAND = "/home/lifthubuser/sbin/jetty-run-lifthub-root.sh"

  val TIMEOUT = 30000
  val KEYWORD_SUCCESS = "INFO::Started"
  //TODO add '======= Backtrace: ========='
  val KEYWORD_FAILURE = "Exception"

  /**
   * Replies a Box[String].
   * This is a blocking operation.
   */
  def receive = {
    case Start(projectName, stopPort) =>
      import scala.util.control.Exception.allCatch
      val cmd = List("sudo", COMMAND, "start", projectName, stopPort.toString)
      self.reply(ResStart(
	allCatch opt {
	  killAll(projectName)
          execute(cmd)
	} match {
	  case Some(_) => checkProcess(projectName)
	  case None => Failure("Failed")
	}
      ))
    case Stop(projectName, stopPort) =>
      val cmd = List(COMMAND, "stop", projectName, stopPort.toString)
      self.reply(ResStop(tryo {
        execute(cmd)
	"Succeeded to stop %s.".format(projectName)
      }))
    case Clean(projectName) =>
      val cmd = List("sudo", COMMAND, "clean", projectName)
      self.reply(ResClean(tryo {
        execute(cmd)
	"Succeeded to clean up %s.".format(projectName)
      }))
  }

  /**
   * TODO Implement this.
   */
  def kill(projectName: String) = {
    val args = List("kill", projectName)
  }

  def execute(cmd: List[String]) = {
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
  def checkProcess(projectName: String): Box[String] = {
    def parseLog(projectName: String): Box[Boolean] = {
      //val log = scala.io.Source.fromFile(serverInfo.executeLogPath).mkString
      val log = scala.io.Source.fromFile(ServerInfo.JAIL_LOG_DIR).mkString
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
	kill(projectName)
        return  Failure("Timeout.")
      }
      parseLog(projectName) match {
        case Full(true) =>
          return  Full("Server started.")
        case Full(false) =>
          kill(projectName)
          return  Failure("An exception occured.")
        case Empty =>
          println("still running") //Debug
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
  def killAll(projectName: String) = {
    //TODO Implement this.
    // For now, kill the process of the pid in the pid file. 
    kill(projectName)
  }

}


}
}
