package net.lifthub {
package serverl {

import akka.actor.Actor
import akka.actor.Actor._
import akka.config.Supervision._
import akka.util.Logging
import akka.dispatch.Dispatchers

import java.util.concurrent.ThreadPoolExecutor._

import net.lifthub.common.event._
import net.lifthub.common.ActorConfig._


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
      println("starting " + projectId)
      self.reply(Response.STARTED)
    case Stop(projectId) => 
      println("stopping " + projectId)
      self.reply(Response.STOPPED)
    case _ => log.slf4j.info("error")
  }
}

object ServerManagerRunner {
  import net.lifthub.common.ActorConfig._
  def run = {
    Actor.remote.start(SERVER_HOST, SERVER_PORT)
    Actor.remote.register(REGISTER_NAME, actorOf[ProjectStatusMonitor])
  }

  def main(args: Array[String]) = run
}


}
}
