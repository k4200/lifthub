package net.lifthub {
package model {

import scala.xml.{ NodeSeq, Text }

import _root_.net.liftweb.mapper._
import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import Helpers._
import net.liftweb.http._
import net.liftweb.sitemap._
import Loc._

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

  // ---------------- invitation -----------------------
  // https://gist.github.com/780788

  // Disable the user creation menu.
  override def createUserMenuLoc: Box[Menu] = Empty

  override lazy val sitemap: List[Menu] =
    List(loginMenuLoc, logoutMenuLoc, createUserMenuLoc,
       lostPasswordMenuLoc, resetPasswordMenuLoc,
       editUserMenuLoc, changePasswordMenuLoc,
       validateUserMenuLoc, inviteMenuLoc).flatten(a => a)

  override lazy val ItemList: List[MenuItem] =
    List(MenuItem(S.??("sign.up"), signUpPath, false),
       MenuItem(S.??("log.in"), loginPath, false),
       MenuItem(S.??("lost.password"), lostPasswordPath, false),
       MenuItem("", passwordResetPath, false),
       MenuItem(S.??("change.password"), changePasswordPath, true),
       MenuItem(S.??("log.out"), logoutPath, true),
       MenuItem(S.??("edit.profile"), editPath, true),
       MenuItem("", validateUserPath, false),
       MenuItem(S.??("invite"), invitePath, false))


  def inviteSuffix = "invite"
  lazy val invitePath = thePath(inviteSuffix)

  def inviteMenuLoc: Box[Menu] =
    Full(Menu(Loc("Invite", invitePath, S.??("invite"), inviteMenuLocParams)))

  protected def inviteMenuLocParams: List[LocParam[Unit]] =
    //snarfLastItem is defined in ProtoUser.
    //Template(() => wrapIt(invite(snarfLastItem))) ::
    //Template(() => invite(S.request)) ::
    Template(() => wrapIt(invite(S.request))) ::
    If(superUser_? _, S.??("requires.superuser")) ::
    Nil

  /**
   * 
   */
  def invite(request: Box[Req]): NodeSeq = {
    val theUser = createNewUserInstance
    def testInvite() {
      validateInvite(theUser) match {
        case Nil =>
          theUser.save
          sendValidationEmail(theUser)
          S.notice(S.??("invite.finish"))
          S.redirectTo(homePage)
        case xs => S.error(xs) ; innerInvite _
      }
    }
    def innerInvite = bind("user",
                      inviteXhtml(theUser),
                      "submit" -> SHtml.submit(S.??("invite"), testInvite _))
    innerInvite
  }

  def validateInvite(user: TheUserType): List[FieldError] = user.validate

  def inviteXhtml(user: TheUserType) = {
    // The same code as the signupXhtml except the label.
    //TODO How to specify password?
    (<form method="post" action={S.uri}>
       <table>
         <tr><td colspan="2">{ S.??("invite") }</td></tr>
          {localForm(user, false, signupFields)}
         <tr><td>&nbsp;</td><td><user:submit/></td></tr>
        </table>
     </form>)
  }
}

/**
 * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class User extends MegaProtoUser[User] {
  //TODO Move
  val MAX_NUM_PROJECTS = 1

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

  /**
   *
   */
  def registerSshKey: Unit = {
    //TODO This doesn't seem to register a new key to the repository.
    GitosisHelper.createSshKey(this)
    GitosisHelper.gitAddSshKey(this)
    GitosisHelper.commitAndPush("Registered a new ssh key of the user " + id, true)
  }

  /**
   * 
   */
  def maxNumProjects: Int = {
    if(this.superUser_?) 10
    else MAX_NUM_PROJECTS
  }
}

}
}
