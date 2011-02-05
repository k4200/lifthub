package net.lifthub {
package model {

import net.liftweb._
import common._
import mapper._
import http.S

import net.lifthub.model._
import net.lifthub.lib._

object Project extends Project with LongKeyedMetaMapper[Project]
with UserEditableCRUDify[Long, Project]
{
  override def dbTableName = "projects"; // define the DB table name
//  override def fieldOrder = List(name, dateOfBirth, url)

  override def beforeSave = List(project =>  {
    //project.
  })

  override def afterSave = List(project =>  {
    User.find(By(User.id, project.userId)) match {
      case Full(user) => 
        val projectInfo = ProjectInfo(project)
        ProjectHelper.createProject(projectInfo, user)
      case _ => println("error...")
    }
  })
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

//   object databaseType extends MappedEnum[Project, DbType.type](this, DbType) {
//     override def dbColumnName = "database_type"
//   }
}


}
}
