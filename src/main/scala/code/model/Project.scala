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

  override def afterSave = List(project =>  {
    User.find(By(User.id, project.userId)) match {
      case Full(user) => 
        val projectInfo = ProjectInfo("", TemplateType.Mvc, "2.2")
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

  //import net.lifthub.lib.TemplateType
  object liftType extends MappedEnum(this, TemplateType) {
  //object liftType extends MappedInt(this) {
    override def dbColumnName = "lift_type"
  }

  object liftVersion extends MappedString(this, 10) {
    override def dbColumnName = "lift_version"
  }
  object databaseType extends MappedInt(this) {
    override def dbColumnName = "database_type"
  }
}


}
}
