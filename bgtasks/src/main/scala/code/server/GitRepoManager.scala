package net.lifthub {
package server {

import akka.actor.Actor
import akka.actor.Actor._
import akka.config.Supervision._
import akka.util.Logging
import akka.dispatch.Dispatchers

import java.util.concurrent.ThreadPoolExecutor._

import bootstrap.liftweb.Boot

import net.lifthub.common.event.gitrepo._
import net.lifthub.common.ActorConfig
import net.lifthub.lib.GitoriousHelper

//import net.lifthub.model.Project._


class GitRepoManager extends Actor {
  // max 5 retries, within 5000 millis
  //self.faultHandler = OneForOneStrategy(List(classOf[Exception]), 5, 5000)

  val name = ActorConfig("gitrepomanager").get.name

  self.dispatcher =
    Dispatchers.newExecutorBasedEventDrivenDispatcher(name)
      //.withNewThreadPoolWithBoundedBlockingQueue(100)
      //.setCorePoolSize(16)
      //.setMaxPoolSize(128)
      //.setKeepAliveTimeInMillis(60000)
      .setRejectionPolicy(new CallerRunsPolicy) // OK?
      .build

  //TODO Implement this
  def receive = {
    case AddUser(userId, email, password) =>
      self.reply(GitoriousHelper.addUser(userId, email, password))
    case RemoveUser(user) => print("....")
    case AddProject(project, user) => print("....")
    case RemoveProject(project) => print("....")
    case AddSshKey(gitoriousUserId, sshKey) =>
      self.reply(GitoriousHelper.addSshKey(gitoriousUserId, sshKey))
    case RemoveSshKey(user) => print("....")
    case _ => log.slf4j.error("Not implemented yet.")
  }
}




object GitRepoManagerRunner {
  import net.lifthub.common.ActorConfig

  def initLiftMapper = {
    val boot = new Boot
    boot.boot
  }

  def run = {
    ActorConfig("gitrepomanager").map { x =>
      Actor.remote.start(x.host, x.port)
      Actor.remote.register(x.name, actorOf[GitRepoManager])
    } getOrElse {
      //TODO
      print("couldn't get the config values for GitRepoManager.")
    }
  }

  def main(args: Array[String]) = {
    initLiftMapper
    run
  }
}




}
}
