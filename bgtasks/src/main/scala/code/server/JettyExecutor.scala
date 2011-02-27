package net.lifthub {
package server {

import java.io._

import net.liftweb.common._
import net.liftweb.util.Helpers._

import akka.actor.Actor
import akka.actor.Actor._

import org.apache.commons.exec._

import internalevent._

import net.lifthub.lib.ServerInfo
import net.lifthub.lib.FileUtils

class JettyExecutor extends Actor {
  val TIMEOUT = 30000

  val keywordSuccess = "INFO::Started"
  val keywordFailure = "Exception"

  /**
   * Replies a Box[String].
   * This method blocks.
   */
  def receive = {
    case Start(serverInfo) =>
      self.reply(tryo {
        start(serverInfo)
      })
    case Stop(serverInfo) =>
      val args = List("stop", serverInfo.projectName, serverInfo.stopPort.toString)
      self.reply(tryo {
        stop(serverInfo)
	Full("stopped")
      })
  }

  def start(server: ServerInfo): Box[String] = {
    val args = List("-DSTOP.PORT=" + server.stopPort,
                    "-DSTOP.KEY=" + server.projectName,
                    "-jar", "start.jar",
                    server.confPath)

    val cmdLine = new CommandLine("java")
    args.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor
    executor.setWorkingDirectory(new File(server.basePath))

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)

    val os = new PipedOutputStream()
    val streamHandler = new PumpStreamHandler(os)
    executor.setStreamHandler(streamHandler)

    val resultHandler = new DefaultExecuteResultHandler()
    executor.execute(cmdLine, resultHandler) // asynchronous
    
    val is = new PipedInputStream(os);
    //val br = new BufferedReader(new InputStreamReader(is)) 
    val inSource = scala.io.Source.fromInputStream(is)

    // Parse the line to see the process has succeeded or falied.
    def parseOutput(line: String): Box[Boolean] = {
      if (line.contains(keywordFailure)) {
        Full(false)
      } else if (line.contains(keywordSuccess)) {
        Full(true)
      } else {
	Empty
      }
    }

    val returnResult = (in: Box[Boolean]) => {
      in match {
	case Full(true) =>
          return Full("Succedded to start.")
	case Full(false) =>
	  watchdog.destroyProcess
	  return Failure("An exception occured during startup. aborted.")
        case _ => {}
      }
    }

//     var line = br.readLine
//     while (line != null) {
//       line = br.readLine
//     }

    // Read the output and write to the log file until a keyword is found.
    FileUtils.printToFile(new File(server.executeLogPath))(writer => {
      for (line <- inSource.getLines) {
        writer.write(line)
        returnResult(parseOutput(line))
      }
    })

    // This shouldn't happen.
    return Failure("The process finished accidentally?")
  }

  def stop(server: ServerInfo): Box[String] = {
    val args = List("-DSTOP.PORT=" + server.stopPort,
                    "-DSTOP.KEY=" + server.projectName,
                    "-jar", "start.jar",
                    "--stop")

    val cmdLine = new CommandLine("java")
    args.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor
    executor.setWorkingDirectory(new File(server.basePath))

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)

    val streamHandler = new PumpStreamHandler(
      new FileOutputStream(new File(server.executeLogPath)))
    executor.setStreamHandler(streamHandler)

    val ret = executor.execute(cmdLine)
    if (ret == 0) { // synchronous
      Full("Succeeded to stop.")
    } else {
      Failure("Failed to stop with exit code " + ret)
    }
  }
}

object Test {
  val TIMEOUT = 10000
  val keywordSuccess = "INFO::Started"
  val keywordFailure = "Exception"


  def start(server: ServerInfo): Box[String] = {
    val args = List("-DSTOP.PORT=" + server.stopPort,
                    "-DSTOP.KEY=" + server.projectName,
                    "-jar", "start.jar",
                    server.confPath)

    val cmdLine = new CommandLine("java")
    args.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor
    executor.setWorkingDirectory(new File(server.basePath))

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)

    val os = new PipedOutputStream()
    val streamHandler = new PumpStreamHandler(os)
    executor.setStreamHandler(streamHandler)

    val resultHandler = new DefaultExecuteResultHandler()
    executor.execute(cmdLine, resultHandler) // asynchronous
    
    val is = new PipedInputStream(os);
    //val br = new BufferedReader(new InputStreamReader(is)) 
    val inSource = scala.io.Source.fromInputStream(is)

    // Parse the line to see the process has succeeded or falied.
    def parseOutput(line: String): Box[Boolean] = {
      if (line.contains(keywordFailure)) {
        Full(false)
      } else if (line.contains(keywordSuccess)) {
        Full(true)
      } else {
	Empty
      }
    }

    val returnResult = (in: Box[Boolean]) => {
      in match {
	case Full(true) =>
          println("!!!!!!!!!!!!!!!!!!!!!!!1")
          return Full("Succedded to start.")
	case Full(false) =>
          watchdog.destroyProcess
          println("killed? " + watchdog.killedProcess)
          streamHandler.stop
          inSource.close
          os.close
	  println("!!!!!!!!!!!!!!!!!!!!!!!2")
	  return Failure("An exception occured during startup. aborted.")
        case _ => {}
      }
    }

    // Read the output and write to the log file until a keyword is found.
    FileUtils.printToFile(new File(server.executeLogPath))(writer => {
      for (line <- inSource.getLines) {
	println("%%%%" + line)
        writer.write(line)
        returnResult(parseOutput(line))
      }
    })

    // This shouldn't happen.
    return Failure("The process finished accidentally?")
  }

  def stop(server: ServerInfo): Box[String] = {
    val args = List("-DSTOP.PORT=" + server.stopPort,
                    "-DSTOP.KEY=" + server.projectName,
                    "-jar", "start.jar",
                    "--stop")

    val cmdLine = new CommandLine("java")
    args.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor
    executor.setWorkingDirectory(new File(server.basePath))

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)

    val streamHandler = new PumpStreamHandler(
      new FileOutputStream(new File(server.executeLogPath)))
    executor.setStreamHandler(streamHandler)

    val ret = executor.execute(cmdLine)
    if (ret == 0) { // synchronous
      Full("Succeeded to stop.")
    } else {
      Failure("Failed to stop with exit code " + ret)
    }
  }

  def main(args: Array[String]) {
    import net.liftweb.common._
    import net.liftweb.mapper._
    import net.lifthub.model.Project
    import net.lifthub.lib.ServerInfo
    import bootstrap.liftweb.Boot
    
    val boot = new Boot
    boot.boot

    val project = Project.find(By(Project.id, 1)).get
    val serverInfo = ServerInfo(project)
    start(serverInfo)
    println("done!!!!!!!!!!!!!!!!!!!!!!!")
  }
}

}
}
