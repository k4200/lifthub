package net.lifthub {
package model {

import scala.io.Source
import scala.collection.JavaConversions._

import java.io.File

import org.apache.commons.io.{FileUtils => CommonsFileUtils}
import org.apache.commons.io.filefilter.WildcardFileFilter

import net.lifthub.lib.ServerInfo
import net.lifthub.lib.ProjectInfo


/**
 * TODO Looks redundant. Maybe creating a helper object is enough.
 */
abstract class LogFile(val file: File) {
  def this(path: String) = {
    this(new File(path))
  }
  def mkString: String = {
    Source.fromFile(file).mkString
  }
}

object LogFile {
  implicit def logfile2file(log: LogFile): File = log.file
}

trait MetaLogFile[T <: LogFile] {
  def all(dir: File, pattern: String = "*"): List[T]
}

// -------------------------

class ServerLog(serverInfo: ServerInfo)
extends LogFile(serverInfo.executeLogPath) {
}

class SbtLog(projectInfo: ProjectInfo)
extends LogFile(projectInfo.sbtLogPath) {
}

class AccessLog(f: File) extends LogFile(f) {
}

object AccessLog extends MetaLogFile[AccessLog] {
  def all(dir: File, pattern: String = "*request.log"): List[AccessLog] = {
    val fileFilter = new WildcardFileFilter(pattern)
    val result = {
      CommonsFileUtils.iterateFiles(dir, fileFilter, null).map( f =>
        new AccessLog(f)
      )
    }
    result.toList
  }

  /**
   * Returns all the access log files of the given server.
   */
  def all(serverInfo: ServerInfo): List[AccessLog] = {
    all(new File(serverInfo.logDirPath))
  }
}



}
}
