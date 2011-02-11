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

class ProjectOperations {
  def listAll: CssBindFunc = {
    ".project *" #> 
      Project.findAllOfOwner.map(p =>
	".name *" #> p.name &
        ".update *" #> SHtml.ajaxButton(Text("Update"), () => update(p)) &
        ".build *" #> SHtml.ajaxButton(Text("Build"), () => build(p)) &
        ".process *" #> "" &
        ".process [href]" #> ""
      )
  }

  def update(project: Project): JsCmd = {
    SbtHelper.update(project) match {
      case Full(x) => S.notice(x)
      case Failure(x, _, _) => S.error(x.toString)
      case Empty => S.error("build failed.")
    }
    Noop
  }

  def build(project: Project): JsCmd = {
    SbtHelper.update(project) match {
      case Full(x) => S.notice(x)
      case Failure(x, _, _) => S.error(x.toString)
      case Empty => S.error("build failed.")
    }
    Noop
  }

}

}
}
