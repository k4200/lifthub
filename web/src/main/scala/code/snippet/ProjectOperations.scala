package net.lifthub {
package snippet {

import _root_.scala.xml.{NodeSeq, Text}

import _root_.net.liftweb._
import util._
import mapper._
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
    ".project *" #> 
      Project.findAllOfOwner.map(p => {
	".name *" #> p.name &
        ".pull *" #> SHtml.ajaxButton(Text("Pull"), () => pull(p)) &
        ".update *" #> SHtml.ajaxButton(Text("Update"), () => update(p)) &
        ".build *" #> SHtml.ajaxButton(Text("Build"), () => build(p)) &
        ".deploy *" #> SHtml.ajaxButton(Text("Deploy"), () => deploy(p)) &
        ".process [id]" #> ("process-" + p.id.toString) &
        ".process *" #> processButton(p) &
        ".process [href]" #> ""
      })
  }
  
  def processButton(p: Project): NodeSeq = {
    import net.lifthub.model.Project._
    if (p.status == Status.Stopped) {
      SHtml.ajaxButton(Text("Start"), () => start(p))
    } else if (p.status == Status.Running) {
      SHtml.ajaxButton(Text("Stop"), () => stop(p))
    } else {
      Text("")
    }
  }

  def changeButton(project: Project): JsCmd = {
    val newproject = Project.find(By(Project.id, project.id)).get
    SetHtml("process-" + project.id, processButton(newproject))
  }

  def pull(project: Project): JsCmd = {
    if(ProjectHelper.pullProject(project)) {
      S.notice("git pull succeeded.")
    } else {
      S.error("git pull failed.")
    }
    Noop
  }

  def execute(project: Project, name: String, func: Project => Box[Any]): JsCmd = {
    func(project) match {
      case Full(x) => S.notice(x.toString)
      case Failure(x, _, _) => S.error(x.toString)
      case Empty => S.error(name + " failed.")
    }
    Noop
  }

  def update(project: Project): JsCmd = {
    execute(project, "update", SbtHelper.update)
  }

  def build(project: Project): JsCmd = {
    execute(project, "build", SbtHelper.makePackage)
  }

  def deploy(project: Project): JsCmd = {
    execute(project, "deploy", SbtHelper.deploy)
  }

  def start(project: Project): JsCmd = {
    execute(project, "start", ServerManagerClient.startServer)
    changeButton(project)
  }

  def stop(project: Project): JsCmd = {
    execute(project, "stop", ServerManagerClient.stopServer)
    changeButton(project)
  }

}

}
}
