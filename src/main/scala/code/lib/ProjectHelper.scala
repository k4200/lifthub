package net.lifthub {
package lib {

import net.lifthub.model.User

import FileUtils._

// JGit
import org.eclipse.jgit._
import lib._
import api.Git
import storage.file.{FileRepository, FileRepositoryBuilder}
import transport.{Transport, RefSpec, RemoteConfig}

import java.io._


/**
 *
 */
object TemplateType extends Enumeration {
  val Basic = TemplateTypeVal("Basic")
  val Blank = TemplateTypeVal("Blank")
  val Mvc   = TemplateTypeVal("MVC")
  val Xhtml = TemplateTypeVal("XHTML")
  case class TemplateTypeVal(name: String) extends Val(name) {
    val dirName = "lift_" + name.toLowerCase
  }
  implicit def valueToTemplateTypeValue(v: Value): TemplateTypeVal
    = v.asInstanceOf[TemplateTypeVal]
}

/**
 * Project information
 * TODO Add databaseType: MySQL, PostgreSQL etc.
 * TODO Merge this into Project.
 */
case class ProjectInfo (name: String, templateType: TemplateType.Value, version: String) {
  //import ProjectInfo._
  def templatePath: String = ProjectInfo.templateBasePath + "/lift_" +
    version + "_sbt/" + templateType.dirName
  def path: String = ProjectInfo.projectBasePath + "/" + name

  //TODO
  //val gitRepoRemote: String = "gitosis@lifthub.net:" + name + ".git"
  val gitRepoRemote: String = "gitosis@www.lifthub.net:" + name + ".git"
}
object ProjectInfo {
  //Paths
  val basePath = "/home/lifthub" //TODO Move somewhere
  val templateBasePath = basePath + "/projecttemplates"
  val projectBasePath = basePath + "/userprojects"

  import net.lifthub.model.Project
  def apply(project: Project): ProjectInfo = {
    this(project.name, project.templateType.is, "2.2")
  }
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
 * TODO non thread safe
 */
object GitosisHelper {
  val adminEmail = "lifthub@localhost.localdomain"

  val gitosisAdminPath = ProjectInfo.basePath + "/gitosis-admin"
  val keydirName = "keydir"
  val keydir = Path(gitosisAdminPath, keydirName)
  lazy val conf = Path(gitosisAdminPath, "gitosis.conf")
  val gitAdminRepoRemote = "gitosis@localhost:gitosis-admin.git"
  //val gitAdminRepoRemote = "gitosis@localhost:gitosis-admin.git"

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
    // gitweb = yes -> the project appears in the list.
    """[group %s]
      |members = %s %s
      |writable = %s"""
      .stripMargin.format(projectInfo.name,
                          adminEmail, user.email, projectInfo.name)
//     """[group %s]
//       |members = %s %s
//       |writable = %s
//       |
//       |[repo %s]
//       |owner = %s"""
//       .stripMargin.format(projectInfo.name,
//                           adminEmail, user.email, projectInfo.name,
//                           projectInfo.name, user.email)
  }

  /**
   * Adds the conf file to the list of the files to be committed.
   */
  def gitAddConf(): Boolean = {
    val git = new Git(gitosisRepo)
    git.add().addFilepattern(conf.relativePath).call()
    true
  }

  def createSshKey(user: User): Boolean = {
    FileUtils.printToFile(keyFile(user))(writer => {
      writer.write(user.sshKey)
    })
  }

  def gitAddSshKey(user: User): Boolean = {
    val git = new Git(gitosisRepo)
    val dirCache = git.add().addFilepattern(keyFile(user).relativePath).call()
    //val dirCache = git.add().addFilepattern("fail").call()
    true
  }

  /**
   *
   */
  def commitAndPush(message: String, dryRun: Boolean = false): Boolean = {
    val git = new Git(gitosisRepo)
    git.commit().setMessage(message).call()

    val refSpec = new RefSpec("refs/heads/master")
    git.push().setRefSpecs(refSpec).setRemote(gitAdminRepoRemote).setDryRun(dryRun).call()
    true
  }

  // for debug
  private def dumpConfig(config: Config, remote: String, name: String) {
    println("----" + name)
    config.getStringList("remote", remote, name).foreach(println)
  }

  // for debug
  private def pushTest {
    try {
      //val remote = gitAdminRepoRemote
      val remote = "origin"
      val config = gitosisRepo.getConfig
      val remoteConfig = new RemoteConfig(config, remote)
      //val transports = Transport.openAll(gitosisRepo, remote, Transport.Operation.PUSH);
      val transport = Transport.open(gitosisRepo, remote)

      println("Repo ->" + gitosisRepo)
      println("remote ->" + remote)
      println(gitosisRepo.getConfig)
      dumpConfig(config, remote, "url")
      dumpConfig(config, remote, "pushurl")
      dumpConfig(config, remote, "fetch")
      dumpConfig(config, remote, "push")
      println(remoteConfig.getURIs().isEmpty() && remoteConfig.getPushURIs().isEmpty())
      println(transport)

      val refSpec = new RefSpec("refs/heads/master")
      val git = new Git(gitosisRepo)
      val dryRun = false
      git.push().setRefSpecs(refSpec).setDryRun(dryRun).setRemote(gitAdminRepoRemote).call()
      //git.push().setRefSpecs(refSpec).setDryRun(dryRun).setRemote(remote).call()
    } catch {
      case e: Exception => e.getCause.printStackTrace
    }
  }
}

// ------------------------------------------------

/**
 */
object ProjectHelper {

  // for debug
  def main(args: Array[String]) {
    val projectInfo = ProjectInfo("foo", TemplateType.Mvc, "2.2")
    val user = new User
    user.email.set("kashima@shibuya.scala-users.org")
    user.sshKey.set("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIEAjz+vWAw0gf7PGUBkVO12HEuDzId08c/uv2kGQmhA7GRZ+Aw8SMhVAua3Vy7Ob21AhWkPfE/1/oiVTWTZSUhuoGtcxcP+0lL13GB5DHABr6eWH9CE11qxBAYs/wk+c7xMMj3Igh2MZvTydVr1useq4f1npiJ8+bzCMJiSKtNhHcs= kashima@shibuya.scala-users.org")
    
    addUserToGitosis(projectInfo, user)
    copyTemplate(projectInfo)
    commitAndPushProject(projectInfo)
  }

  def createProject(projectInfo: ProjectInfo, user: User) = {
    addUserToGitosis(projectInfo, user)
    copyTemplate(projectInfo)
    commitAndPushProject(projectInfo)
  }

  def addUserToGitosis(projectInfo: ProjectInfo, user: User): Boolean = {
    GitosisHelper.addEntry2Conf(projectInfo, user) &&
    GitosisHelper.gitAddConf() &&
    GitosisHelper.createSshKey(user) &&
    GitosisHelper.gitAddSshKey(user) &&
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

  def commitAndPushProject(projectInfo: ProjectInfo,
                           dryRun: Boolean = false): Boolean = {
    val builder = new FileRepositoryBuilder()
    val projectRepo = 
      builder.setGitDir(projectInfo.path + "/" + Constants.DOT_GIT)
      .readEnvironment().findGitDir().build()
    
    try {
      val git = Git.init.setDirectory(new File(projectInfo.path)).call()
      git.add().addFilepattern(".").call()
      git.commit().setMessage("New project").call()

      val refSpec = new RefSpec("refs/heads/master")
      git.push().setRefSpecs(refSpec).setDryRun(dryRun).setRemote(projectInfo.gitRepoRemote).call()
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
