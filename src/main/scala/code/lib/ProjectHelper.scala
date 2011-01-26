package net.lifthub {
package lib {

import net.lifthub.model.User

import FileUtils._

// JGit
import org.eclipse.jgit._
import lib._
import api.Git
import storage.file.{FileRepository, FileRepositoryBuilder}

import java.io._


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

  val gitRepoRemote: String = "gitosis@lifthub.net:" + name + ".git"
}
object ProjectInfo {
  //Paths
  val basePath = "/home/lifthub" //TODO Move somewhere
  val templateBasePath = basePath + "/projecttemplates"
  val projectBasePath = basePath + "/userprojects"
}

// ------------------------------------------------
case class Path(base: String, relativePath: String)
extends File (new File(base), relativePath) {
  // just to be consistent
  def getRelativePath = relativePath
  def + (toAppend: String): Path = 
    Path(base, relativePath + File.separator + toAppend)
}

/**
 * Utilities that handle gitosis
 */
object GitosisHelper {
  val adminEmail = "lifthub@localhost.localdomain"

  val gitosisAdminPath = ProjectInfo.basePath + "/gitosis-admin"
  val keydirName = "keydir"
  val keydir = Path(gitosisAdminPath, keydirName)
  lazy val conf = Path(gitosisAdminPath, "gitosis.conf")

  def keyFileName(user: User): String = user.email + ".pub"
  def keyFile(user: User): Path = keydir + keyFileName(user)

  // Initialize RepositoryBuilder
  lazy val builder = new FileRepositoryBuilder()
  lazy val gitosisRepo = 
    builder.setGitDir(gitosisAdminPath + "/" + Constants.DOT_GIT)
    .readEnvironment().findGitDir().build()

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
    FileUtils.printToFile(keyFile(user))(writer => {
      writer.write(user.sshKey)
    })
  }

  def addSshKeyToGit(user: User): Boolean = {
    val git = new Git(gitosisRepo)
    val dirCache = git.add().addFilepattern(keyFile(user).relativePath).call()
    //val dirCache = git.add().addFilepattern("fail").call()
//     if (dirCache.lock && dirCache.commit) {
//       dirCache.unlock
//       true
//     } else {
//       false
//     }
    true
  }

  /**
   *
   */
  def commitAndPush(message: String): Boolean = {
    val git = new Git(gitosisRepo)
    git.commit().setMessage(message).call()
    git.push().call()
    true
  }
}

// ------------------------------------------------

/**
 */
object ProjectHelper {
  def createProject(projectInfo: ProjectInfo, user: User) = {
    addUserToGitosis(projectInfo, user)
    copyTemplate(projectInfo)
    commitAndPushProject(projectInfo)
  }

  def addUserToGitosis(projectInfo: ProjectInfo, user: User): Boolean = {
    GitosisHelper.addEntry2Conf(projectInfo, user) &&
    GitosisHelper.createSshKey(user) &&
    GitosisHelper.addSshKeyToGit(user) &&
    GitosisHelper.commitAndPush("Added a user: " + user.email)
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

  def commitAndPushProject(projectInfo: ProjectInfo): Boolean = {
    val builder = new FileRepositoryBuilder()
    val projectRepo = 
      builder.setGitDir(projectInfo.path + "/" + Constants.DOT_GIT)
      .readEnvironment().findGitDir().build()
    
    try {
      val git = Git.init.setDirectory(new File(projectInfo.path)).call()
      git.add().addFilepattern(".").call()
      git.commit().setMessage("New project").call()
      // so far so good

//       val config = new Config
//       val remoteConfig = new RemoteConfig(config, "origin")
//       remoteConfig.addPushURI(new URIsh(projectInfo.gitRepoRemote))

      //projectInfo.gitRepoRemote
      // setRemote -> origin
      //git.push().setRemote().call()
      git.push().setRemote(projectInfo.gitRepoRemote).call()
    } catch {
      case e: Exception  =>
        e.printStackTrace
        println(e.getCause)
        return false
    }
    true
  }

}

}
}
