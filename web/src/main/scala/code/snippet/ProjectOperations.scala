package net.lifthub {
package snippet {

import _root_.scala.xml.{NodeSeq, Text}

import _root_.net.liftweb._
import util._
import common._
import http._
import js._
import JsCmds._
import Helpers._

import _root_.java.util.Date

import net.lifthub.model.Project
import net.lifthub.lib._
import net.lifthub.client._

class ProjectOperations {
  def listAll: CssBindFunc = {
    import net.lifthub.model.Project._
    ".project *" #> 
      Project.findAllOfOwner.map(p => {
	val process = {
          if (p.status == Status.Stopped) {
            SHtml.ajaxButton(Text("Start"), () => start(p))
          } else if (p.status == Status.Running) {
            SHtml.ajaxButton(Text("Stop"), () => stop(p))
          } else {
            Text("")
          }
	}
	".name *" #> p.name &
        ".pull *" #> SHtml.ajaxButton(Text("Pull"), () => pull(p)) &
        ".update *" #> SHtml.ajaxButton(Text("Update"), () => update(p)) &
        ".build *" #> SHtml.ajaxButton(Text("Build"), () => build(p)) &
        ".deploy *" #> SHtml.ajaxButton(Text("Deploy"), () => deploy(p)) &
        ".process *" #> process &
        ".process [href]" #> ""
      })
  }

  def pull(project: Project): JsCmd = {
    if(ProjectHelper.pullProject(project)) {
      S.notice("git pull succeeded.")
    } else {
      S.error("git pull failed.")
    }
    Noop
  }

  //TODO repetition
  def update(project: Project): JsCmd = {
    SbtHelper.update(project) match {
      case Full(x) => S.notice(x)
      case Failure(x, _, _) => S.error(x.toString)
      case Empty => S.error("build failed.")
    }
    Noop
  }

  def build(project: Project): JsCmd = {
    SbtHelper.makePackage(project) match {
      case Full(x) => S.notice(x)
      case Failure(x, _, _) => S.error(x.toString)
      case Empty => S.error("build failed.")
    }
    Noop
  }

  def deploy(project: Project): JsCmd = {
    SbtHelper.deploy(project) match {
      case Full(x) => S.notice(x)
      case Failure(x, _, _) => S.error(x.toString)
      case Empty => S.error("deploy failed.")
    }
    Noop
  }

  def start(project: Project): JsCmd = {
    ServerManagerClient.startServer(project) match {
      case Full(x) => S.notice(x)
      case Failure(x, _, _) => S.error(x.toString)
      case Empty => S.error("unknown error...")
    }
    Noop
  }

  def stop(project: Project): JsCmd = {
    ServerManagerClient.stopServer(project) match {
      case Full(x) => S.notice(x)
      case Failure(x, _, _) => S.error(x.toString)
      case Empty => S.error("unknown error...")
    }
    Noop
  }

}

}
}
