package net.lifthub {
package model {

import _root_.net.liftweb.mapper._
import _root_.net.liftweb.util._
import _root_.net.liftweb.common._

import net.lifthub.lib.GitosisHelper


/**
 * The singleton that has methods for accessing the database
 */
object User extends User with MetaMegaProtoUser[User] {
  override def dbTableName = "users" // define the DB table name
  override def screenWrap = Full(<lift:surround with="default" at="content">
			       <lift:bind /></lift:surround>)
  // define the order fields will appear in forms and output
  override def fieldOrder = List(id, firstName, lastName, email,
  locale, timezone, password, sshKey)

  override def editFields = super.editFields ::: List(sshKey)

  // comment this line out to require email validations
  //override def skipEmailValidation = true

  //KK By default, Lift uses the hostname in the request.
  // For example, if the user was submitting a request to a page
  // at http://192.168.0.10/somepage, and that page invoked the
  // email feature, the emailFrom Lift generates would be
  // "noreply@192.168.0.10", which would be a problem.
  override def emailFrom = "noreply@lifthub.net"

  //
  override def beforeUpdate = List(user => {
    //If the key is different from the current one.
    if (user.sshKey.is.length > 0 && user.sshKey.dirty_?) {
      println("new ssh key!" + user.sshKey)
      user.registerSshKey
    } else {
      println("ssh key hasn't been changed.")
    }
  })

}

/**
 * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class User extends MegaProtoUser[User] {
  def getSingleton = User // what's the "meta" server

  //Just for testing when I was answering the question on the URL below.
  // http://stackoverflow.com/questions/4210169/scala-lift-remove-locale-and-time-zone-from-sign-up/4715542#4715542
//   import _root_.scala.xml.Text
//   override lazy val locale = new MyLocale(this) {
//     override val fieldId = Some(Text(null))
//   }

  object sshKey extends MappedTextarea(this, 1024) {
    override def dbColumnName = "ssh_key"
    override def textareaRows  = 10
    override def textareaCols = 50
    override def displayName = "SSH public key"
  }

  def registerSshKey: Unit = {
    GitosisHelper.createSshKey(this)
    GitosisHelper.gitAddSshKey(this)
    GitosisHelper.commitAndPush("Registered a new ssh key of the user " + id, true)
  }
}

}
}
