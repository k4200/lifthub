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

import net.lifthub.model.{LogFile, AccessLog, ServerLog, SbtLog}
import net.lifthub.model.Project
import net.lifthub.lib._

class Log {
  def listProject: CssBindFunc = {
    def showMenu(project: Project)() = {
      //S.set("project_id", project.id.toString)
      //projectId = project.id
      S.redirectTo("/log?project_id=" + project.id)
    }
    currentProject match {
      case Empty =>
	".project *" #>
          Project.findAllByCurrentUser.map(p => {
            ".button *" #> SHtml.submit(p.name, showMenu(p))
          })
      case _ => ".project *" #> List[NodeSeq]()
    }
  }


  // ----------------------------

  def logMenu: CssBindFunc = {
    currentProject match {
      //case Empty => ".log_menu *" #> List[NodeSeq]() & ClearClearable
      case Empty => ClearClearable
      case _ => ".dummy" #> ""
     }
   }

  def otherLogs: CssBindFunc = {
    currentProject match {
      case Full(p) =>
        //TODO Should be moved to SeverInfo and ProjectInfo respectively?
        val serverLog = new ServerLog(ServerInfo(p))
        val sbtLog = new SbtLog(ProjectInfo(p))
        ".server *" #> SHtml.ajaxButton(Text("server"), () => showLog(serverLog)) &
        ".sbt *" #> SHtml.ajaxButton(Text("sbt"), () => showLog(sbtLog))
      case _ => ClearClearable
    }
  }

  def accessLog: CssBindFunc = {
    currentProject match {
      case Full(p) =>
        val si = ServerInfo(p)
        ".access_log *" #> 
          AccessLog.all(si).map(a => {
            ".name *" #> a.getPath &
            ".show *" #> SHtml.ajaxButton(Text("Show"), () => showLog(a))
          })
      case _ => ".access_log *" #>  List[NodeSeq]()
    }
  }

  def showLog(log: LogFile): JsCmd = {
    import scala.io.Source
    SetHtml("log",
            try {
              Text(Source.fromFile(log.getPath).mkString)
            } catch {
              case _ => Text("")
            })
  }

  lazy val currentProject: Box[Project] = {
    (for(projectIdStr <- S.param("project_id");
         projectId <- tryo{projectIdStr.toLong})
    yield {
      // If valid project_id is passed, finds the project.
      // This may return Empty
      println("project_id = " + projectId)
      val res = Project.findByCurrentUser(By(Project.id, projectId))
      if(res.size == 1) {
	Full(res(0))
      } else {
	Empty
      }
    }).getOrElse {
      // If project_id isn't specified and the user has only one project,
      // return it. Or, return Empty.
      val all = Project.findAllByCurrentUser
      println("all.size = " + all.size)
      if (all.size == 1) {
        Full(all(0))
      } else {
        Empty
      }
    }
  }
}

}
}
