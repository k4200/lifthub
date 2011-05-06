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
import net.lifthub.common.event.gitrepo.response._
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

  def receive = {
    case AddUser(userId, email, password) =>
      val res = GitoriousHelper.addUser(userId, email, password)
      self.reply(ResAddUser(res))
    case RemoveUser(gitoriousUserId) =>
      val res = GitoriousHelper.removeUser(gitoriousUserId)
      self.reply(ResRemoveUser(res))
    case AddProject(gitoriousUserId, projectName) =>
      val res = GitoriousHelper.addProject(gitoriousUserId, projectName)
      self.reply(ResAddProject(res))
    case RemoveProject(projectId) =>
      val res = GitoriousHelper.removeProject(projectId)
      self.reply(ResRemoveProject(res))
    case AddSshKey(gitoriousUserId, sshKey) =>
      val res = GitoriousHelper.addSshKey(gitoriousUserId, sshKey)
      self.reply(ResAddSshKey(res))
    case RemoveSshKey(gitoriousUserId) =>
      val res = GitoriousHelper.removeSshKey(gitoriousUserId)
      self.reply(ResRemoveSshKey(res))
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
