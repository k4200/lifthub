package net.lifthub {
package model {

import net.liftweb._
import util.{FieldError,FieldIdentifier,StringValidators}
import common._
import mapper._
import http.S

import scala.xml.Text

import net.lifthub.model._
import net.lifthub.lib._

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
    if (project.name == "www") {
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
          project.userId(user.id)
	}
      case _ =>
        List(FieldError(Project.name,
                        Text(S.??("validation.general.require.login"))))
    }
  }

  override def afterCreate = List(project =>  {
    (for(dbInfo <-project.database.obj;
    user <- User.find(By(User.id, project.userId)))
    yield {
      val projectInfo = ProjectInfo(project)
      ProjectHelper.createProject(projectInfo, user)
      ProjectHelper.createProps(projectInfo, dbInfo)
      ProjectHelper.commitAndPushProject(projectInfo)

      // Copy the jail template and create a config file for jetty.
      val serverInfo = ServerInfo(project)
      serverInfo.setupNewServer

      // nginx
      val nginxConf = NginxConf(project)
      nginxConf.writeToFile
    }) getOrElse {
      println("error...") //TODO rollback
    }
  })

  override def afterDelete = List(project => {
    println("afterDelete")

    // Drop the database.
    for(database <- project.database.obj)
    yield {
      database.dropDatabase
      database.delete_!
    }

    // Remove the entry from gitosis.
    val pi = ProjectInfo(project)
    GitosisHelper.removeEntryFromConf(pi)
    GitosisHelper.gitAddConf
    GitosisHelper.commitAndPush("Remove project " + project.name)

    //TODO Remove the repository itself.
    // This requires "gitosis" user privilege, so be careful.

    // Delete the server environment.
    val si = ServerInfo(project)
    si.deleteServer

    // Remove the project files.
    ProjectHelper.deleteProject(pi)
  })

  private[model] def getAvailablePort: Int = {
    val maxport = max(port)
    if (maxport != 0) maxport.toInt + 1
    else PORT_RANGE._1
  }
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

  object templateType extends MappedEnum[Project, TemplateType.type](this, TemplateType) {
    override def dbColumnName = "lift_template_type"
  }

  object liftVersion extends MappedString(this, 10) {
    override def dbColumnName = "lift_version"
    override def defaultValue = "2.2" 
    override def dbDisplay_? = false //TODO for now only 2.2 is available.
  }

  object database extends MappedLongForeignKey(this, UserDatabase) {
    //TODO should be done automatically.
    override def validSelectValues = Full(
      (0L, "Not Selected") ::
      (User.currentUser match {
	case Full(user) =>
	  UserDatabase.findAll(By(UserDatabase.userId, user.id)).map {
	    x => (x.id.is, x.databaseType.is + ":" + x.name.is)
         }
	case _ => Nil
      })
    )
  }

  /**
   * Port number on which the server (currently jetty) runs.
   * 9000-9999 are used for now.
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

  // The below methods are a temporary solution.
  lazy val info = ProjectInfo(this)
  lazy val server = ServerInfo(this)
}


}
}
