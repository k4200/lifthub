package net.lifthub {
package lib {

import java.io._

import net.liftweb.common._
import net.liftweb.util._
import Helpers._

import net.lifthub.model.User
import net.lifthub.model.Project
import net.lifthub.model.ProjectTemplate

import org.apache.commons.io.{FileUtils => CommonsFileUtils}


import FileUtils._

// JGit
import org.eclipse.jgit._
import lib._
import api.Git
import storage.file.{FileRepository, FileRepositoryBuilder}
import transport.{Transport, RefSpec, RemoteConfig, URIish}


case class ServerInfo(projectName: String, port: Int, version: String) {
  //Paths
  val JAIL_SETUP_PROG = Props.get("jailer.path.bin.setup") openOr
    "/home/lifthub/sbin/setup-jail.sh" // requires root privilege
  //TODO should be JAILER_PARENT_DIR ?
  val JAIL_PARENT_DIR = Props.get("jailer.path.jailroot") openOr
    "/home/lifthubuser/chroot"
  //TODO jetty-6
  val JAILER_TEMPLATE_DIR = Props.get("") openOr
    "/home/lifthubuser/servers/jetty-6/etc"

  val jailRootPath = JAIL_PARENT_DIR + "/" + projectName

  val JAIL_SERVER_DIR = Props.get("jail.path.serverroot") openOr
    "/home/lifthubuser/servers"
  //TODO only jetty-6
  //  "/home/lifthubuser/logs"
  val JAIL_LOG_DIR = Props.get("jail.path.log") openOr
    "/home/lifthubuser/servers/jetty-6/logs"
  //TODO only jetty-6, not a constant
  val JAIL_WEBAPP_DIR = Props.get("jail.path.webappdir") openOr
    "/home/lifthubuser/servers/jetty-6/userwebapps/" + projectName
  //TODO jetty-6
  val JAIL_CONF_DIR = Props.get("jail.path.confdir") openOr
    "/home/lifthubuser/servers/jetty-6/etc/lifthub"

  //TODO
  val serverName = "jetty"
  val basePath = JAIL_SERVER_DIR + "/%s-%s".format(serverName, version)

  val deployDirPath = jailRootPath + JAIL_WEBAPP_DIR
  val confPath = jailRootPath + JAIL_CONF_DIR + "/" + projectName + ".xml"

  val templatePath = jailRootPath + JAILER_TEMPLATE_DIR + "/jetty.xml.tmpl"
  val logDirPath = jailRootPath + JAIL_LOG_DIR
  val executeLogPath = logDirPath + "/" + projectName + "-execute.log"

  //val pidFilePath = jailRootPath + basePath + "/logs/" + projectName + ".pid"
  val stopPort = port + 1000 //TODO

  /**
   * Sets up a new server runtime environment.
   *
   */
  def setupNewServer = {
    executeJailSetupProgram("create")
    writeConfFile
  }

  def deleteServer = {
    executeJailSetupProgram("delete")
  }

  /**
   * Creates a config file for the application server.
   * Currently, only jetty is supported.
   * This must be called after the chroot is created.
   */
  def writeConfFile: Boolean = {
    FileUtils.printToFile(confPath)(writer => {
      writer.write(confString)
    })
  }

  def confString: String = {
    import scala.io.Source
    val tmpl = Source.fromFile(templatePath)
    val portPattern = "#port#".r
    val namePattern = "#name#".r
    namePattern.replaceAllIn(
      portPattern.replaceAllIn(tmpl.mkString, port.toString),
      projectName)
  }

  /**
   * Executes the jail setup program with sudo.
   * @parameter cmd either "create" or "delete"
   */
  def executeJailSetupProgram(cmd: String) = {
    //TODO Test this. this may throw an exception.
    import org.apache.commons.exec._
    val cmdLine = new CommandLine("sudo")
    cmdLine.addArgument(JAIL_SETUP_PROG)
    cmdLine.addArgument(cmd)
    cmdLine.addArgument(projectName)

    val executor = new DefaultExecutor
    //executor.setWorkingDirectory(new File(JAIL_PARENT_DIR))
    executor.execute(cmdLine)  // synchronous
  }
}

object ServerInfo {
  def apply(project: Project): ServerInfo = {
    this(project.name, project.port, "6")
  }
}

// ------------------------------------------------
case class NginxConf(projectName: String, port: Int) {
  val confPath = "/home/lifthub/nginx/conf.d/%s.conf".format(projectName)
  val logPath = "/home/lifthub/nginx/logs/%s.access.log".format(projectName)

  def writeToFile(): Boolean = {
    FileUtils.printToFile(confPath)(writer => {
      writer.write(confString)
    })
  }

  def confString: String = {
    """    server {
      |        server_name %s.lifthub.net;
      |        access_log %s main;
      |        location / {
      |            proxy_pass   http://127.0.0.1:%d/;
      |        }
      |    }
      |"""
      .stripMargin.format(projectName, logPath, port)
  }
}
object NginxConf {
  def apply(project: Project): NginxConf = {
    this(project.name, project.port.is)
  }
  def remove(project: Project): Boolean = {
    val nginxConf = NginxConf(project.name, 0)
    new java.io.File(nginxConf.confPath).delete &&
    new java.io.File(nginxConf.logPath).delete
  }
}

// ------------------------------------------------
/**
 * Project information
 * TODO Merge this into Project?
 */
case class ProjectInfo (name: String, projectTemplate: ProjectTemplate) {
  val SCALA_VER = "2.8.1"

  //import ProjectInfo._
  def templatePath: String = ProjectInfo.templateBasePath + "/" + projectTemplate.path
  def path: String = ProjectInfo.projectBasePath + "/" + name
  /**
   * contains database account information.
   */
  //def propsPath = path + "/src/main/resources/default.props"
  def propsPath = path + "/src/main/resources/props/production.default.props"

  //TODO hard coded
  def warPath = path + ("/target/scala_%s/lift-sbt-template_%s-0.1.war"
                        .format(SCALA_VER, SCALA_VER))

  def sbtLogPath = path + "-sbt.log"

  val gitRepoRemote: String = "gitosis@lifthub.net:" + name + ".git"
}
object ProjectInfo {
  //Paths
  val basePath = "/home/lifthub" //TODO Move somewhere
  val templateBasePath = basePath + "/projecttemplates"
  val projectBasePath = basePath + "/userprojects"

  def apply(project: Project): ProjectInfo = {
    //TODO This may throw an exception...
    this(project.name, project.template.obj.get)
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

// ------------------------------------------------
object SbtHelper {
  import net.liftweb.common._
  import net.liftweb.mapper._
  //import xsbt.Process._
  import net.lifthub.model.Project
  //TODO COR?
  def update(project: Project): Box[String] = {
    //TODO Shold be done at the same time as some other actions.
    runCommand(project, "update")
  }

  def makePackage(project: Project): Box[String] = {
    runCommand(project, "package")
  }

  def deploy(project: Project): Box[String] = {
    //TODO Hot deploy.
    val pi = ProjectInfo(project)
    val si = ServerInfo(project)
    if (!(new File(pi.warPath)).exists) {
      return Failure("war file doesn't exist. Build first.")
    }
    tryo {
      CommonsFileUtils.copyFile(pi.warPath, si.deployDirPath + "/ROOT.war")
      pi.warPath.delete
      Full("Project %s successfully deployed.".format(project.name))
    } openOr {
      Failure("Failed to deploy.")
    }
  }

  def workspaceExists_?(pi: ProjectInfo): Boolean = {
    new File(pi.path).exists
  }

  def runCommand(project: Project, command: String): Box[String] = {
    import org.apache.commons.exec._
    val pi = ProjectInfo(project)

    if (!workspaceExists_?(pi)) {
      return Failure("Workspace doesn't exist. Maybe, you need to update workspace first?")
    }

    val executor = new DefaultExecutor
    executor.setWorkingDirectory(pi.path)
    // Note that this 'sbt' is not in the one included in the project
    // but /home/lifthub/bin/sbt
    val cmdLine = new CommandLine("sbt")
    cmdLine.addArgument(command)
    val streamHandler = new PumpStreamHandler(
      new FileOutputStream(new File(pi.sbtLogPath)))
    executor.setStreamHandler(streamHandler)
    tryo {
      val resultCode = executor.execute(cmdLine)  // synchronous
      if (resultCode == 0) {
	Full("'sbt %s' succeeded.".format(command))
      } else {
	Failure("'sbt %s' returned %d.".format(command, resultCode))
      }
    } openOr {
      Failure("failed to start 'sbt %s'.".format(command))
    }
  }

  def main(args: Array[String]) {
    import bootstrap.liftweb.Boot
    val boot = new Boot
    boot.boot

    println("SbtHelper.main")
    for(project <- Project.find(By(Project.id, 1)))
    yield
      SbtHelper.update(project)
  }

}

// ------------------------------------------------
/**
 */
object ProjectHelper {
  // for debug
  def main(args: Array[String]) {
    import net.liftweb.mapper._
    import bootstrap.liftweb.Boot
    val boot = new Boot
    boot.boot

    val project = Project.find(By(Project.id, 2)).get
    updateWorkspace(project)
  }

  /**
   * Adds the user to gitosis and copy the template.
   * The changes need to be committed by using
   * <code>commitAndPushProject</code>.
   */
  def createProject(projectInfo: ProjectInfo, user: User) = {
    addUserToGitosis(projectInfo, user)
    copyTemplate(projectInfo)
    //commitAndPushProject(projectInfo)
  }

  def addUserToGitosis(projectInfo: ProjectInfo, user: User): Boolean = {
    GitosisHelper.addEntry2Conf(projectInfo, user) &&
    GitosisHelper.gitAddConf() &&
    GitosisHelper.createSshKey(user) &&
    GitosisHelper.gitAddSshKey(user) &&
    GitosisHelper.commitAndPush("Added a user: " + user.email)
  }

  def deleteProject(projectInfo: ProjectInfo) = {
    CommonsFileUtils.deleteDirectory(projectInfo.path)
  }

  def updateWorkspace(project: Project): Box[String] = {
    tryo {
      val pi = ProjectInfo(project)
      deleteProject(pi)
      cloneProject(pi)
      "Updating workspace succeeded."
    }
  }

  //TODO shoud be private --------
  def copyTemplate(projectInfo: ProjectInfo): Boolean = {
    try {
      CommonsFileUtils.copyDirectory(projectInfo.templatePath, projectInfo.path)
//       val sbt = projectInfo.path + "/sbt"
//       sbt.setExecutable(true)
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

      //
      val config = projectRepo.getConfig
      val remoteConfig = new RemoteConfig(config, "origin")
      val refSpec = new RefSpec("refs/heads/master")
      remoteConfig.addPushRefSpec(refSpec)
      remoteConfig.addFetchRefSpec(refSpec)
      remoteConfig.addURI(new URIish(projectInfo.gitRepoRemote))
      remoteConfig.update(config)

      config.setString("branch", "master", "remote", "origin")
      config.setString("branch", "master", "merge", "refs/heads/master")
      config.save

      git.add().addFilepattern(".").call()
      git.commit().setMessage("New project").call()

      git.push().setRefSpecs(refSpec).setDryRun(dryRun).setRemote(projectInfo.gitRepoRemote).call()
    } catch {
      case e: Exception  =>
        e.printStackTrace
        println(e.getCause)
        return false
    }
    true
  }

  //TODO Write test cases.
  def cloneProject(projectInfo: ProjectInfo) = {
    val destDir = projectInfo.path
    Git.cloneRepository.setURI(projectInfo.gitRepoRemote)
      .setDirectory(destDir).call
  }

  /**
   * @deprecated
   */
  def pullProject(project: Project): Boolean = {
    val projectInfo = ProjectInfo(project)
    val builder = new FileRepositoryBuilder()
    val projectRepo = 
      builder.setGitDir(projectInfo.path + "/" + Constants.DOT_GIT)
      .readEnvironment().findGitDir().build()
    
    try {
      val git = new Git(projectRepo)

//       val refSpec = new RefSpec("refs/heads/master")
//       git.push().setRefSpecs(refSpec).setDryRun(dryRun).setRemote(projectInfo.gitRepoRemote).call()
      git.pull().call()
    } catch {
      case e: Exception  =>
        e.printStackTrace
        println(e.getCause)
        return false
    }
    true
  }

  import net.lifthub.model.UserDatabase
  /**
   * Creates a properties file for db connection
   */
  def createProps(projectInfo: ProjectInfo, dbInfo: UserDatabase): Boolean = {
    val propsFile = new File(projectInfo.propsPath)
    FileUtils.printToFile(propsFile)(writer => {
      writer.write(generatePropsString(dbInfo))
    })
    true
  }

  /**
   *
   */
  def generatePropsString(dbInfo: UserDatabase): String = {
    (for(password <- dbInfo.plainPassword)
    yield
    """db.driver=%s
      |db.url=jdbc:%s://%s/%s
      |db.user=%s
      |db.password=%s"""
      .stripMargin.format(dbInfo.databaseType.is.driver,
                          "mysql", dbInfo.hostname.is, dbInfo.name.is,
                          dbInfo.username, password)
    )getOrElse ""

  }

}

}
}
