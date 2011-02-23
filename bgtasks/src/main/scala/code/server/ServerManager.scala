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

import net.lifthub.common.ActorConfig._
import net.lifthub.lib.ServerInfo
import net.lifthub.model.Project

//import org.apache.commons.exec._

import bootstrap.liftweb.Boot

object ServerManagerCore {
  val executor = actorOf[JettyExecutor]
  executor.start

  import internalevent._
  def start(server: ServerInfo) = {
    executor ! Start(server)
  }

  def stop(server: ServerInfo) = {
    executor ! Stop(server)
  }

//   def execute2(server: ServerInfo, command: List[String]) = {
//     val cmdLine = new CommandLine(command.head)
//     command.tail.foreach(cmdLine.addArgument _)
//     val resultHandler = new DefaultExecuteResultHandler()
//   }

  def execute(server: ServerInfo, command: List[String]) = {
    val pb = (new java.lang.ProcessBuilder(command: _*)) directory
      new java.io.File(server.basePath)
    tryo {
      // Don't wait for the process to finish.
      val proc = pb.start
      Full("Maybe started.")
    } openOr {
      Failure("failed to start 'sbt %s'.".format(command))
    }
  }

//   def stop(server: ServerInfo) = {
//     val command = List("java", "-DSTOP.PORT=" + server.stopPort,
//                        " -DSTOP.KEY=" + server.projectName,
//                        "-jar", "start.jar",
//                        "--stop")
//     execute(server, command)
//   }
}

class ServerManager extends Actor {
  // max 5 retries, within 5000 millis
  //self.faultHandler = OneForOneStrategy(List(classOf[Exception]), 5, 5000)

  self.dispatcher =
    Dispatchers.newExecutorBasedEventDrivenDispatcher(REGISTER_NAME)
      //.withNewThreadPoolWithBoundedBlockingQueue(100)
      //.setCorePoolSize(16)
      //.setMaxPoolSize(128)
      //.setKeepAliveTimeInMillis(60000)
      .setRejectionPolicy(new CallerRunsPolicy) // OK?
      .build

  import net.lifthub.common.event._
  import net.lifthub.model.Project._
  def receive = {
    case Start(projectId) => 
      Project.find(By(Project.id, projectId)) match {
        case Full(project) =>
          val server = ServerInfo(project)
          ServerManagerCore.start(server)
          project.status(Status.Running)
          project.save
          self.reply(Response.STARTED)
          println("started " + projectId)
        case _ =>
          //TODO ERROR  
          self.reply(Response.FAILED)
          println("failed to start " + projectId)
      }
    case Stop(projectId) => 
      Project.find(By(Project.id, projectId)) match {
        case Full(project) =>
          val server = ServerInfo(project)
          ServerManagerCore.stop(server)
          project.status(Status.Stopped)
          project.save
          self.reply(Response.STOPPED)
        case _ =>
          //TODO ERROR  
          self.reply(Response.FAILED)
          println("failed to stop " + projectId)
      }
    case _ => log.slf4j.info("error")
  }
}

object ServerManagerRunner {
  import net.lifthub.common.ActorConfig._

  def initLiftMapper = {
    val boot = new Boot
    boot.boot
  }

  def run = {
    Actor.remote.start(SERVER_HOST, SERVER_PORT)
    Actor.remote.register(REGISTER_NAME, actorOf[ServerManager])
  }

  def main(args: Array[String]) = {
    initLiftMapper
    run
  }
}


}
}
