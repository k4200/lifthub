package net.lifthub {
package server {

import java.io.File

import akka.actor.Actor
import akka.actor.Actor._

import org.mortbay.jetty.Server
import org.mortbay.xml.XmlConfiguration

import internalevent._

import net.lifthub.lib.ServerInfo

class JettyExecutor extends Actor {
  lazy val server = new Server

  def receive = {
    case Start(serverInfo) =>
      start(serverInfo)
    case Stop(serverInfo) =>
      println("stopped.")
  }

  def start(serverInfo: ServerInfo) = {
    val config = new XmlConfiguration(new File(serverInfo.confPath).toURL)
    config.configure(server)
    server.start
  }

  def stop(serverInfo: ServerInfo) = {
  }

}

}
}
