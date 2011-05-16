package net.lifthub {
package server {

import net.liftweb.util.Helpers._
import net.liftweb.common._
import net.liftweb.mapper._

import akka.actor.Actor
import akka.actor.Actor._
import akka.config.Supervision._
import akka.util.Logging
import akka.dispatch.Dispatchers

import java.util.concurrent.ThreadPoolExecutor._

import net.lifthub.common.ActorConfig
import net.lifthub.common.event.server.response._
import net.lifthub.lib.ServerInfo
import net.lifthub.lib.FileUtils
import net.lifthub.model.Project

//import org.apache.commons.exec._

import bootstrap.liftweb.Boot

/**
 * Sends messages to Actors for different servers, such as
 * Jetty and Tomcat.
 * Looks a bit redundant.
 */
object ServerManagerCore {
  import internalevent._
  val TIMEOUT = 60000

  val jettyExecutor = actorOf[JettyExecutor]
  jettyExecutor.start

  def unknownResponse = {
    Failure("unknown response...")
  }
  
  //TODO only jetty (stopPort)
  def start(projectName: String, stopPort: Int): Box[String] = {
    val executor = jettyExecutor
    convertResult(executor !! (Start(projectName, stopPort), TIMEOUT))
  }

  //TODO only jetty (stopPort)
  def stop(projectName: String, stopPort: Int): Box[String] = {
    val executor = jettyExecutor
    convertResult(executor !! (Stop(projectName, stopPort), TIMEOUT))
  }

  def clean(projectName: String): Box[String] = {
    val executor = jettyExecutor
    convertResult(executor !! (Clean(projectName), TIMEOUT))
  }

  /**
   * Converts results from the executor to Box.
   */
  //def convertResult(result: Option[Any]): Box[Any] = {
  def convertResult(result: Option[Any]): Box[String] = {
    result match {
      case Some(Full(x)) => x match {
	case s: String => Full(s)
	case _ => Failure("unknown result")
      }
      case Some(Failure(x,y,z)) => Failure(x,y,z)
      case Some(_) => Failure("This shouldn't happen.")
      // This shouldn't happen either because the executor checks
      // time after it started a server, and replies Failure if
      // timeout occurs.
      case None => Failure("timeout") 
    }
  }
}


/**
 * TODO Move
 */
object RuntimeEnvironmentHelper {
  import net.lifthub.lib.FileUtils._

  def create(projectName: String, port: Int): Box[String] = {
    import net.lifthub.lib.NginxConf
    //writeConfFile(serverInfo)
    val nginxConf = NginxConf(projectName, port)
    if (nginxConf.writeToFile) {
      executeJailSetupProgram("create", projectName)
    } else {
      Failure("Failed to write an nginx conf file for project " + projectName)
    }
  }

  def delete(projectName: String): Box[String] = {
    executeJailSetupProgram("delete", projectName)
  }

  /**
   * Creates a config file for the application server.
   * Currently, only jetty is supported.
   * @deprecated
   * Config file for the new arch doesn't contain project
   * specific values.
   */
  def writeConfFile(serverInfo: ServerInfo): Boolean = {
    FileUtils.printToFile(serverInfo.confPath)(writer => {
      writer.write(serverInfo.confString)
    })
  }

  /**
   * Executes the jail setup program with sudo.
   * @parameter cmd either "create" or "delete"
   */
  def executeJailSetupProgram(cmd: String, projectName: String): Box[String] = {
    //TODO Test this. this may throw an exception.
    import org.apache.commons.exec._
    val cmdLine = new CommandLine("sudo")
    cmdLine.addArgument(ServerInfo.JAIL_SETUP_PROG)
    cmdLine.addArgument(cmd)
    cmdLine.addArgument(projectName)

    val executor = new DefaultExecutor
    //executor.setWorkingDirectory(new File(ServerInfo.JAIL_PARENT_DIR)) //TODO
    tryo {
      val st = executor.execute(cmdLine)  // synchronous
      "%s %s succeeded. (result code: %d)".format(cmd, projectName, st)
    }
  }
}


/**
 * This actor runs as a service and listens on the port specified
 * in ActorConfig.
 */
class ServerManager extends Actor {
  val TIMEOUT = 90000 //TODO hard-coded

  // max 5 retries, within 5000 millis
  //self.faultHandler = OneForOneStrategy(List(classOf[Exception]), 5, 5000)

  val name = ActorConfig("servermanager").get.name

  self.dispatcher =
    Dispatchers.newExecutorBasedEventDrivenDispatcher(name)
      //.withNewThreadPoolWithBoundedBlockingQueue(100)
      //.setCorePoolSize(16)
      //.setMaxPoolSize(128)
      //.setKeepAliveTimeInMillis(60000)
      .setRejectionPolicy(new CallerRunsPolicy) // OK?
      .build

  import net.lifthub.common.event.server._
  import net.lifthub.model.Project._
  def receive = {
    case Start(projectName) => 
      self.reply(ResStart(
	ServerManagerCore.start(projectName, TIMEOUT)))
    case Stop(projectName) => 
      self.reply(ResStop(
	ServerManagerCore.stop(projectName, TIMEOUT)))
    case Clean(projectName) => 
      self.reply(ResClean(
	ServerManagerCore.clean(projectName)))
    case Create(projectName, port) =>
      self.reply(ResCreate(
	RuntimeEnvironmentHelper.create(projectName, port)))
    case Delete(projectName) =>
      self.reply(ResDelete(
	RuntimeEnvironmentHelper.delete(projectName)))
    case _ => log.slf4j.info("unknown message")
  }
}

object ServerManagerRunner {
  import net.lifthub.common.ActorConfig

  def initLiftMapper = {
    val boot = new Boot
    boot.boot
  }

  def run = {
    ActorConfig("servermanager").map { x =>
      Actor.remote.start(x.host, x.port)
      Actor.remote.register(x.name, actorOf[ServerManager])
    } getOrElse {
      //TODO
      print("couldn't get the config values for ServerManager.")
    }
  }

  def main(args: Array[String]) = {
    initLiftMapper
    run
  }
}


}
}
