package net.lifthub {
package lib {

import java.io.FileWriter

import org.apache.commons.io.{FileUtils => CommonsFileUtils}

// JGit
import org.eclipse.jgit._
import lib._
import api.Git
import storage.file.{FileRepository, FileRepositoryBuilder}
import transport.{Transport, RefSpec, RemoteConfig, URIish}

import FileUtils._
import net.lifthub.model.User




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
   * Removes the entry of the project from gitosis.conf
   */
  def removeEntryFromConf(projectInfo: ProjectInfo): Boolean = {
    val tempFile = java.io.File.createTempFile("gitosis", "conf")
    import scala.io.Source
    
    val startPattern = "^\\[group %s\\]$".format(projectInfo.name).r

    FileUtils.printToFile(tempFile)(writer => {
      val it = Source.fromFile(conf).getLines()
      for (line <- it) {
	startPattern.findFirstIn(line) match {
          case Some(_) =>
            while (it.hasNext && it.next.length != 0) {}
          case _ =>
            writer.println(line)
	}
      }
    })
    
    try {
      CommonsFileUtils.copyFile(tempFile, conf)
      tempFile.delete
      true
    } catch {
      case e: Exception =>
        e.printStackTrace
        false
    }
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

  def main(args: Array[String]) {
    val pi = ProjectInfo("foo", TemplateType.Mvc, "2.2")
    removeEntryFromConf(pi)
  }
}

}
}
