package net.lifthub {
package serverl {

import net.liftweb.util.Helpers._
import net.liftweb.common._
import net.liftweb.mapper._

import akka.actor.Actor
import akka.actor.Actor._
import akka.config.Supervision._
import akka.util.Logging
import akka.dispatch.Dispatchers

import java.util.concurrent.ThreadPoolExecutor._

import net.lifthub.common.event._
import net.lifthub.common.ActorConfig._
import net.lifthub.lib.ServerInfo
import net.lifthub.model.Project

import bootstrap.liftweb.Boot

object ServerManagerCore {
  def start(server: ServerInfo) = {
    val command = List("java", "-DSTOP.PORT=" + server.stopPort,
                       " -DSTOP.KEY=" + server.projectName,
                       "-jar", "start.jar",
                       server.confPath)
    execute(server, command)
  }

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

  def stop(server: ServerInfo) = {
    val command = List("java", "-DSTOP.PORT=" + server.stopPort,
                       " -DSTOP.KEY=" + server.projectName,
                       "-jar", "start.jar",
                       "--stop")
    execute(server, command)
  }
}

class ProjectStatusMonitor extends Actor {
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

  def receive = {
    case Start(projectId) => 
      Project.find(By(Project.id, projectId)) match {
        case Full(project) =>
          val server = ServerInfo(project)
          ServerManagerCore.start(server)
          self.reply(Response.STARTED)
          println("started " + projectId)
        case _ =>
          //TODO ERROR  
          self.reply(Response.STARTED)
          println("failed to start " + projectId)
      }
    case Stop(projectId) => 
      println("stopping " + projectId)
          //ServerManagerCore.start(server)
      self.reply(Response.STOPPED)
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
    Actor.remote.register(REGISTER_NAME, actorOf[ProjectStatusMonitor])
  }

  def main(args: Array[String]) = {
    initLiftMapper
    run
  }
}


}
}
