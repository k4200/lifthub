package net.lifthub {
package model {


import net.liftweb._
import util.{FieldError,FieldIdentifier,StringValidators}
import util.BindHelpers._
import common._
import mapper._
import http.S

import net.lifthub.lib._

object ProjectTemplate extends ProjectTemplate
with LongKeyedMetaMapper[ProjectTemplate]
with AdminEditableCRUDify[Long, ProjectTemplate]
{
  override def dbTableName = "project_templates"; // define the DB table name
}


class ProjectTemplate extends LongKeyedMapper[ProjectTemplate]
with IdPK
with AdminEditableKeyedMapper[Long, ProjectTemplate]
{
  def getSingleton = ProjectTemplate
  override val userObject = User

  object name extends MappedString(this, 40) {
    override def validations = valUnique(S.??("unique.projecttemplate.name")) _ :: super.validations
  }

  /**
   * Relative path to the project template.
   */
  object path extends MappedString(this, 40) {
  }

  /**
   * Not really used at this moment, but maybe useful.
   */
  object liftVersion extends MappedString(this, 10) {
    override def dbColumnName = "lift_version"
  }


}



}
}
