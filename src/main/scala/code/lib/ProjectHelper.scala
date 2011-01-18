package net.lifthub {
package lib {

case class TemplateType(name: String) {
  val dirName = "lift_" + name.toLowerCase
}
//implicit def str2templateType(s: String) = TemplateType(s)
case object TemplateType {
  val Basic = TemplateType("Basic")
  val Blank = TemplateType("Blank")
  val Mvc   = TemplateType("MVC")
  val Xhtml = TemplateType("XHTML")
  val templateTypes = List(Basic, Blank, Mvc, Xhtml)
}

/**
 * Project information
 * TODO Add databaseType: MySQL, PostgreSQL etc.
 */
case class ProjectInfo (name: String, templateType: TemplateType, version: String) {
  def templatePath: String = ProjectInfo.templateBasePath + "/lift_" +
    version + "_sbt/" + templateType.dirName
  def path: String = ProjectInfo.projectBasePath + "/" + name
}
object ProjectInfo {
  //Paths
  val basePath = "/home/lifthub"
  val templateBasePath = basePath + "/projecttemplates"
  val projectBasePath = basePath + "/userprojects"
  val gitosisAdminPath = basePath + "/gitosis-admin"
}



object ProjectHelper {

  import net.lifthub.model.User
  def createProject(projectInfo: ProjectInfo, user: User) = {
    copyTemplate(projectInfo)
  }
  
  //TODO shoud be private
  def copyTemplate(projectInfo: ProjectInfo) = {
    //TODO stub
    "cp " + projectInfo.templatePath + " " + projectInfo.path
  }


}

}
}
