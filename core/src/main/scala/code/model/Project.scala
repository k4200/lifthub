package net.lifthub {
package model {

import scala.xml.NodeSeq

import net.liftweb._
import util.{FieldError,FieldIdentifier,StringValidators}
import util.BindHelpers._
import common._
import mapper._
import http.S

import scala.xml.Text

import net.lifthub.model._
import net.lifthub.lib._
import net.lifthub.client.GitRepoManagerClient

object Project extends Project with LongKeyedMetaMapper[Project]
with UserEditableCRUDify[Long, Project]
with AggregateFunctions[Project]
{
  object Status extends Enumeration {
    val Stopped = StatusVal("Stopped")
    val Starting = StatusVal("Starting")
    val Running = StatusVal("Running")
    val Stopping = StatusVal("Stopping")
    case class StatusVal(name: String) extends Val(name) {
      //
    }
    implicit def valueToStatusValue(v: Value): StatusVal
      = v.asInstanceOf[StatusVal]
  }

  // Constant values
  val PORT_RANGE = (9000, 9999)

  override def dbTableName = "projects"; // define the DB table name
//  override def fieldOrder = List(name, dateOfBirth, url)

  override def validation = List(checkNumberOfProjects, checkSshKey, checkSpecialNames)

  override def beforeCreate = List(
    checkNumberOfProjects,
    setPort,
    createDatabaseIfNone
  )

  override def beforeDelete = List(project => {
    import net.lifthub.client._
    println("Project.beforeDelete")
    ServerManagerClient.stopServer(project)
  })

  private def checkNumberOfProjects(project: Project): List[FieldError] = {
    User.currentUser match {
      case Full(user) =>
        if (count(By(Project.userId, user.id)) >= user.maxNumProjects) {
          List(FieldError(Project.name, Text("You can't create more than %d project.".format(user.maxNumProjects))))
        } else {
          Nil
        }
      case _ =>
        List(FieldError(Project.name,
                        Text(S.??("validation.general.require.login"))))
    }
  }

  /**
   * Checks if the user has registered an SSH key.
   * 'project' isn't used, but necessary to be used by validation.
   */
  private def checkSshKey(project: Project): List[FieldError] = {
    User.currentUser match {
      case Full(user) =>
        if(user.sshKey.is.length == 0) {
          List(FieldError(Project.name, Text("You need to register your SSH key first.")))
        } else {
          Nil
        }
      case _ =>
        List(FieldError(Project.name,
                        Text(S.??("validation.general.require.login"))))
    }
  }

  private def checkSpecialNames(project: Project): List[FieldError] = {
    //TODO Temporary
    val ngNames = List("www", "skr", "git", "test", "mysql", "lifthub", "lifthub_test",
                       "information_schema", "basejail", "newjail")
    println(ngNames)
    println(project.name.is)
    if (ngNames.contains(project.name.is)) {
      List(FieldError(Project.name, Text(project.name + " can't be used.")))
    } else {
      Nil
    }
  }

  private def setPort(project: Project): Unit = {
    project.port(getAvailablePort)
  }

  private def createDatabaseIfNone(project: Project): Unit = {
    User.currentUser match {
      case Full(user) =>
        if(project.database == 0) {
          val dbInfo = UserDatabase.createFromProject(project)
          dbInfo.userId(user.id)
          dbInfo.save
          project.database(dbInfo)
          project.userId(user.id) //TODO should be set automatically.
        }
      case _ =>
        List(FieldError(Project.name,
                        Text(S.??("validation.general.require.login"))))
    }
  }

  override def afterCreate = List(project =>  {
    import net.lifthub.client.ServerManagerClient
    (for(dbInfo <-project.database.obj;
    user <- User.find(By(User.id, project.userId)))
    yield {
      val projectInfo = ProjectInfo(project)
      ProjectHelper.copyTemplate(projectInfo)
      ProjectHelper.createProps(projectInfo, dbInfo)
      ProjectHelper.commitAndPushProject(projectInfo)

      ServerManagerClient.create(this)

      // Copy the jail template and create a config file for jetty.
      // It'll be done by flavour, so not neccesary anymore.
      //val serverInfo = ServerInfo(project)
      //serverInfo.setupNewServer

      // nginx
      //TODO Move 
      //val nginxConf = NginxConf(project)
      //nginxConf.writeToFile
    }) getOrElse {
      println("error...") //TODO rollback
    }
  })

  // 
  override def afterSave = List(project => {
    //
    if (project.gitoriousProjectId.is == 0) {
      for(user <- User.find(By(User.id, project.userId));
	  id <- GitRepoManagerClient.addProject(user, project))
      yield {
        project.gitoriousProjectId(id)
        project.save
        //
        //GitRepoManagerClient.addSshKey(user, adminSshKey)
      }
    }
  })

  override def afterDelete = List(project => {
    println("afterDelete")

    // Drop the database unless there are other projects
    // that use the same database,
    for(database <- project.database.obj)
    yield {
      Project.find(By(Project.database, database.id)) match {
        case Empty =>
          if(!"test".equals(System.getProperty("run.mode"))) {  
            database.dropDatabase
	  }
          database.delete_!
        case _ => println("This database is used by other projects.")
      }
    }

    // Remove the project from the git repo.
    GitRepoManagerClient.removeProject(project)


    // Delete the server environment.
    //val si = ServerInfo(project)
    //si.deleteServer

    // Remove the project files.
    ProjectHelper.deleteProject(project.info)

    // Delete the nginx conf file.
    NginxConf.remove(project)
  })

  private[model] def getAvailablePort: Int = {
    val maxport = max(port)
    if (maxport != 0) maxport.toInt + 1
    else PORT_RANGE._1
  }

  // Disable the Edit menu
  override def editMenuLoc = Empty

}

class Project extends LongKeyedMapper[Project]
with IdPK
with UserEditableKeyedMapper[Long, Project]
{
  def getSingleton = Project
  override val userObject = User

  //object user extends MappedLongForeignKey(this, User)

  object name extends MappedString(this, 20) {
    override def validations = valUnique(S.??("unique.project.name")) _ :: super.validations
  }

  object template extends MappedLongForeignKey(this, ProjectTemplate) {
    override def dbNotNull_? = true
    override def validSelectValues = Full(
      ProjectTemplate.findAll().map {
        x => (x.id.is, x.name.is)
      }
    )
  }

  object database extends MappedLongForeignKey(this, UserDatabase) {
    //TODO should be done automatically.
    override def validSelectValues = Full(
      (0L, "Create a new one (recommended)") :: Nil
    )
  }

  /**
   * Port number on which the server (currently jetty) runs.
   * 9000-9999 are used for now.
   * Now, it's used to generate IP address.
   * TODO should retire this.
   */
  object port extends MappedInt(this) {
    /**
     * This field has to be set by the system.
     */
    override def dbDisplay_? = false
    override def validations = valUnique(S.??("validation.project.port")) _ :: super.validations
    def valUnique(msg: => String)(value: Int): List[FieldError] =
    fieldOwner.getSingleton.findAll(By(this,value)).
    filter(!_.comparePrimaryKeys(this.fieldOwner)) match {
      case Nil => Nil
      case x :: _ => List(FieldError(this, Text(msg))) // issue 179
    }
  }

  import Project.Status
  object status extends MappedEnum[Project, Status.type](this, Status) {
    override def dbDisplay_? = false
    override def dbColumnName = "status"
  }

  //
  object gitoriousProjectId extends MappedInt(this) {
    override def dbColumnName = "gitorious_project_id"
    override def dbDisplay_? = false
  }

  // The below methods are a temporary solution.
  lazy val info = ProjectInfo(this)
  lazy val server = ServerInfo(this)
}


}
}
