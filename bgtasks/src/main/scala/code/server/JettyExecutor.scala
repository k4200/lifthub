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
  val COMMAND = "bin/jetty-run-lifthub.sh"
  val TIMEOUT = 30000

  /**
   * Replies a Box[String].
   * This method blocks.
   */
  def receive = {
    case Start(serverInfo) =>
      val args = List("start", serverInfo.projectName, serverInfo.stopPort.toString)
      self.reply(tryo {
        execute(serverInfo, args)
	Full("started")
      })
    case Stop(serverInfo) =>
      val args = List("stop", serverInfo.projectName, serverInfo.stopPort.toString)
      self.reply(tryo {
        execute(serverInfo, args)
	Full("stopped")
      })
  }

  def execute(server: ServerInfo, args: List[String]) = {
    val cmdLine = new CommandLine(COMMAND)
    args.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor
    executor.setWorkingDirectory(new File(server.basePath))

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)

    // Discard the output. (Actually, there's no output from the process
    // because the shell spript redirects it to the log file.)
    val streamHandler = new PumpStreamHandler(null, null, null)

    //val streamHandler = new PumpStreamHandler
//     val streamHandler = new PumpStreamHandler(
//       new FileOutputStream(new File(server.executeLogPath)))

    executor.setStreamHandler(streamHandler)

    val resultHandler = new DefaultExecuteResultHandler()
    executor.execute(cmdLine)  // synchronous

    println("JettyExecutor.execute finished.")
  }

}

object Test {
  val COMMAND = "bin/jetty-run-lifthub.sh"
  val TIMEOUT = 20000

  def main(args: Array[String]) = {
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
  }

  def execute(server: ServerInfo, args: List[String]) = {
    import java.io._
    val cmdLine = new CommandLine(COMMAND)
    args.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor
    executor.setWorkingDirectory(new File(server.basePath))

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)

    // Redirect the output to a file
    //val os = new FileOutputStream(new File(server.executeLogPath))
    val os = new PipedOutputStream()
    val streamHandler = new PumpStreamHandler(os)
    executor.setStreamHandler(streamHandler)

    val resultHandler = new DefaultExecuteResultHandler()

    executor.execute(cmdLine, resultHandler)
//     spawn {
//       println("exec start")
//       val exitValue = executor.execute(cmdLine)
//       //executor.execute(cmdLine, resultHandler)
//       println("exec!!!!!!!!!!!!!!!!")
//     }

    
    val is = new PipedInputStream(os);
    val br = new BufferedReader(new InputStreamReader(is)) 

    var line = br.readLine
    var finish = false
    while (line != null && !finish) {
      println(line)
      if (line.contains("Exception")) {
	println("done!!!!!!!!!!!!!!!!(error)")
	finish = true
      }
      line = br.readLine
    }

//     val is = new DataInputStream(new PipedInputStream(os))
//     val buf = new Array[Byte](1024)
//     while (is.read(buf, 0, 1024) != -1) {
//       print(new String(buf, "UTF-8"))
//     }
    println("done!!!!!!!!!!!!!!!!")
  }
}

}
}
