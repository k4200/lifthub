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

  //TODO only jetty
  val executor = actorOf[JettyExecutor]
  executor.start

  //TODO only jetty (stopPort)
  def start(projectName: String, stopPort: Int): Box[Any] = {
    convertResult(executor !! (Start(projectName, stopPort), TIMEOUT))
  }

  //TODO only jetty (stopPort)
  def stop(projectName: String, stopPort: Int): Box[Any] = {
    convertResult(executor !! (Stop(projectName, stopPort), TIMEOUT))
  }

  def clean(projectName: String): Box[Any] = {
    convertResult(executor !! (Clean(projectName), TIMEOUT))
  }

  /**
   * Converts results from the executor to Box.
   */
  def convertResult(result: Option[Any]): Box[Any] = {
    result match {
      case Some(Full(x)) => Full(x)
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

  def create(projectName: String) = {
    executeJailSetupProgram("create", projectName)
    //writeConfFile(serverInfo)
  }

  def delete(projectName: String) = {
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
      ServerManagerCore.start(projectName, 9000) match { //TODO hard-coded
	case Full(x) =>
	  //TODO Move to monitor program.
          //project.status(Status.Running)
          //project.save
          self.reply(Response.STARTED)
        case Failure(x, _, _) =>
          println(x)
          self.reply(Response.FAILED)
        case _ => unexpectedResult
      }
    case Stop(projectName) => 
      ServerManagerCore.stop(projectName, 9000) match { //TODO hard-coded
        case Full(x) =>
          //project.status(Status.Stopped)
          //project.save
          self.reply(Response.STOPPED)
        case Failure(x, _, _) =>
          println(x)
          self.reply(Response.FAILED)
        case _ => unexpectedResult
      }
    case Clean(projectName) => 
      ServerManagerCore.clean(projectName) match {
        case Full(x) =>
          self.reply(Response.CLEANED_UP)
        case Failure(x, _, _) =>
          println(x)
          self.reply(Response.FAILED)
        case _ => unexpectedResult
      }
    case Create(serverInfo) =>
      self.reply(ResCreate(
	RuntimeEnvironmentHelper.create(serverInfo.projectName)))
    case Delete(serverInfo) =>
      self.reply(ResDelete(
	RuntimeEnvironmentHelper.delete(serverInfo.projectName)))
    case _ => log.slf4j.info("unknown message")
  }
  def unexpectedResult = {
    println("unknown error...")
    self.reply(Response.FAILED)
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
