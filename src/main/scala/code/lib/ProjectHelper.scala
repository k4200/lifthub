package net.lifthub {
package lib {

import net.lifthub.model.User

import FileUtils._


/**
 *
 */
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
  //import ProjectInfo._
  def templatePath: String = ProjectInfo.templateBasePath + "/lift_" +
    version + "_sbt/" + templateType.dirName
  def path: String = ProjectInfo.projectBasePath + "/" + name
}
object ProjectInfo {
  //Paths
  val basePath = "/home/lifthub" //TODO Move somewhere
  val templateBasePath = basePath + "/projecttemplates"
  val projectBasePath = basePath + "/userprojects"
}


// ------------------------------------------------

/**
 * Utilities that handles gitosis
 */
object GitosisHelper {
  import java.io._
  val gitosisAdminPath = ProjectInfo.basePath + "/gitosis-admin"
  val keydirPath = gitosisAdminPath + "/keydir"
  val adminEmail = "lifthub@localhost.localdomain"
  lazy val conf = new File(gitosisAdminPath + "/gitosis.conf")

  /**
   * Adds an entry to gitosis.conf
   */
  def addEntry2Conf(projectInfo: ProjectInfo, user: User): Boolean = {
    FileUtils.printToFile(new FileWriter(conf, true))(writer => {
      writer.println(generateConfEntryString(projectInfo, user))
      writer.println //newLine
    })
  }

  /**
   * Generates a string to be added to the config file.
   */
  def generateConfEntryString(projectInfo: ProjectInfo, user: User): String = {
    // group -> a group of people.
    // writable -> the name of the project to which this group can write.
    """[group %s]
      |members = %s %s
      |writable = %s"""
      .stripMargin.format(projectInfo.name, adminEmail,
                          user.email, projectInfo.name)
  }

  def createSshKey(user: User): Boolean = {
    val keyFilePath = keydirPath + "/" + user.email + ".pub"
    FileUtils.printToFile(new File(keyFilePath))(writer => {
      writer.write(user.sshKey)
    })
  }

}

// ------------------------------------------------

/**
 */
object ProjectHelper {
  val git = "/usr/bin/git"

  def createProject(projectInfo: ProjectInfo, user: User) = {
    addUserToGitosis(projectInfo, user)
    copyTemplate(projectInfo)
  }

  def addUserToGitosis(projectInfo: ProjectInfo, user: User): Boolean = {
    GitosisHelper.addEntry2Conf(projectInfo, user) &&
    GitosisHelper.createSshKey(user)
    //TODO git add keyfile
    //TODO git commit
    //TODO git push
  }

  //TODO shoud be private --------
  def copyTemplate(projectInfo: ProjectInfo): Boolean = {
    import org.apache.commons.io.FileUtils
    try {
      FileUtils.copyDirectory(projectInfo.templatePath, projectInfo.path)
      true
    } catch {
      case e: java.io.IOException => false
    }
  }

  //TODO
  def commitAndPushProject(projectInfo: ProjectInfo) = {
    //TODO git commit -a
    //TODO git push
  }

}

}
}
