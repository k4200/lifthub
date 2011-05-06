package net.lifthub {
package lib {


import java.io._

import net.liftweb.common._
import net.liftweb.util._
import Helpers._

import org.apache.commons.exec._
import org.apache.commons.exec.environment._

import net.lifthub.model._


object GitoriousHelper {
  // /opt/nginx/html/gitorious
  val TIMEOUT = 20000
  val LOGIN_NAME_PREFIX = "lifthub-"

  val rootDir = Props.get("git.gitorious.rootdir") openOr ""

  /**
   *
   * @return user id in Gitorious
   */
  def addUser(userId: Int, email: String, password: String): Box[Int] = {
    // login_name = 'lifthub-' + userid
    // email = 'test@lifthub.net'
    // password = 'secretstr' 

    val args = List(LOGIN_NAME_PREFIX + userId, email, password)
    executeScript("create_user", args)
  }

  /**
   *
   * @return ssh key id in Gitorious 
   */
  def addSshKey(gitoriousUserId: Int, sshKey: String): Box[Int] = {
    // user_id (gitorious)
    // key_file eg. '/tmp/test-ssh-key'

    if (sshKey.length == 0) {
      return Empty
    }

    val tempfile = File.createTempFile("ssh-key", ".pub")
    FileUtils.printToFile(tempfile)(writer => {
      writer.print(sshKey)
    })

    val args = List(gitoriousUserId.toString, tempfile.getAbsolutePath)
    val ret = executeScript("create_key", args)

    tempfile.delete
    ret
  }

  def removeSshKey(user: User): Unit = {
  }

  //TODO Implement this.
  def changePassword(user: User): Unit = {
    println("Not implemented yet.")
  }

  def executeScript(scriptName: String, args: List[String]): Box[Int] = {
    val env = EnvironmentUtils.getProcEnvironment.asInstanceOf[java.util.HashMap[String, String]]
    env.put("RAILS_ENV", "production")

    val cmdLine = new CommandLine("ruby")
    cmdLine.addArgument("script/" + scriptName)
    args.foreach(cmdLine.addArgument _)

    val executor = new DefaultExecutor
    executor.setExitValues(null)

    val watchdog = new ExecuteWatchdog(TIMEOUT)
    executor.setWatchdog(watchdog)
    executor.setWorkingDirectory(new File(rootDir))

    //
    val stdout = new ByteArrayOutputStream() //OK?
    val streamHandler = new PumpStreamHandler(stdout)
    executor.setStreamHandler(streamHandler)

    val resultHandler = new DefaultExecuteResultHandler()
    val exitValue = executor.execute(cmdLine, env)  // synchronous

    if (exitValue != 0) {
      println(stdout.toString) //Debug
      //log.slf4j.error(stdout.toString)
    }

    tryo {
      Integer.parseInt(stdout.toString.split("\n").toList.last)
    }
  }

  // testing
  def main(args: Array[String]) {
    import bootstrap.liftweb.Boot
    val boot = new Boot
    boot.boot
    User.find(1).map { user =>
      addUser(user.id.is.asInstanceOf[Int], user.email.is, user.password.plain)
    }
  }
}


}
}
