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

  //TODO should vary depending on the user (ie. payment status).
  val MAX_NUM_PROJECTS = 1

  override def dbTableName = "projects"; // define the DB table name
//  override def fieldOrder = List(name, dateOfBirth, url)

  override def validation = List(checkNumberOfDatabases)

  override def beforeCreate = List(
    checkNumberOfDatabases,
    setPort,
    createDatabaseIfNone
  )

  private def checkNumberOfDatabases(project: Project): List[FieldError] = {
    User.currentUserId match {
      case Full(userId) =>
        if (count(By(Project.userId, userId.toLong)) < MAX_NUM_PROJECTS) {
          List(FieldError(Project.name, Text("You can't create more than 1 project.")))
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
      //FIXME The file won't be added to git.
      ProjectHelper.createProps(projectInfo, dbInfo)
      // Create a config file for jetty.
      val serverInfo = ServerInfo(project)
      serverInfo.writeConfFile
      //serverInfo
      val nginxConf = NginxConf(project)
      nginxConf.writeToFile
    }) getOrElse {
      println("error...") //TODO rollback
    }
  })

  override def afterDelete = List(project => {
    //TODO Delete project files.
    println("afterDelete")

    for(database <- project.database.obj)
    yield {
      database.dropDatabase
      database.delete_!
    }
  })

  def getAvailablePort: Int = {
    val maxport = max(port)
    if (maxport != 0) maxport.toInt + 1
    else 9000
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
    override def dbColumnName = "status"
  }
}


}
}
