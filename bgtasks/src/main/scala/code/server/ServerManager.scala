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
  //TODO ok?
  val executor = actorOf[JettyExecutor]
  executor.start

  import internalevent._
  def start(server: ServerInfo): Box[Any] = {
    executor !! Start(server) match {
      case Some(reply) => Full(reply)
      case None => Failure("timeout")
    }
  }

  def stop(server: ServerInfo): Box[Any] = {
    executor !! Stop(server) match {
      case Some(reply) => Full(reply)
      case None => Failure("timeout")
    }
  }
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
          ServerManagerCore.start(server) match {
            case Full(x) =>
	      project.status(Status.Running)
	      project.save
	      self.reply(Response.STARTED)
	      println("started " + projectId)
            case Failure(x, _, _) =>
              println(x)
              self.reply(Response.FAILED)
            case _ =>
              println("unknown error...")
              self.reply(Response.FAILED)
          }
        case _ =>
          //TODO ERROR  
          self.reply(Response.FAILED)
          println("failed to start " + projectId)
      }
    case Stop(projectId) => 
      Project.find(By(Project.id, projectId)) match {
        case Full(project) =>
          val server = ServerInfo(project)
          ServerManagerCore.stop(server) match {
            case Full(x) =>
	      project.status(Status.Stopped)
	      project.save
	      self.reply(Response.STOPPED)
            case Failure(x, _, _) =>
              println(x)
              self.reply(Response.FAILED)
            case _ =>
              println("unknown error...")
              self.reply(Response.FAILED)
	  }
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
