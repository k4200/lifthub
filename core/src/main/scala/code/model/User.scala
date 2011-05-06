package net.lifthub {
package model {

import scala.xml.{ NodeSeq, Text, Elem }

import _root_.net.liftweb.mapper._
import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import Helpers._
import Mailer._
import net.liftweb.http._
import net.liftweb.sitemap._
import Loc._

import net.lifthub.lib.GitosisHelper
//import net.lifthub.lib.GitoriousHelper
import net.lifthub.client.GitRepoManagerClient


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
  override def afterSave = List(user => {
    //
    if (user.gitoriousUserId.is == 0) {
      GitRepoManagerClient.addUser(user).map { id =>
        user.gitoriousUserId(id)
        user.save
      }
    }
  })


  // Called before 'update' not 'insert'.
  override def beforeUpdate = {
    def registerSshKeyIfChanged(user: User) = {
      //If the key is different from the current one.
      if (user.sshKey.is.length > 0 && user.sshKey.dirty_?) {
	println("new ssh key:" + user.sshKey.is)
	user.registerSshKey
      } else {
	println("ssh key hasn't been changed.")
      }
    }
    // Changes the password of the account(s) associated with Lifthub,
    // such as Gitorious
    def changeOtherPasswordsIfChanged(user: User) = {
      //If the passwod was changed
      if (user.password.is.length > 0 && user.password.dirty_?) {
	//println("new password" + user.password.plain)
	user.changeGitoriousPassword
      } else {
	println("password hasn't been changed.")
      }
    }
    List(registerSshKeyIfChanged, changeOtherPasswordsIfChanged)

  }

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
    //'wrapIt' wraps the html piece with the template.
    //Template(() => invite(S.request)) ::
    Template(() => wrapIt(invite(S.request))) ::
    If(superUser_? _, S.??("requires.superuser")) ::
    Nil

  /**
   * 
   */
  def invite(request: Box[Req]): NodeSeq = {
    import org.apache.commons.lang.RandomStringUtils
    val theUser = createNewUserInstance
    def testInvite() {
      val plainPassword = RandomStringUtils.randomAlphanumeric(8)
      theUser.password(plainPassword)
      validateInvite(theUser) match {
        case Nil =>
          theUser.save
          sendValidationEmail(theUser, plainPassword)
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

  def inviteFields: List[FieldPointerType] =
    List(firstName, lastName, email)

  def inviteXhtml(user: TheUserType) = {
    // The same code as the signupXhtml except the label.
    (<form method="post" action={S.uri}>
       <table>
         <tr><td colspan="2">{ S.??("invite") }</td></tr>
          {localForm(user, false, inviteFields)}
         <tr><td>&nbsp;</td><td><user:submit/></td></tr>
        </table>
     </form>)
  }

  /**
   * Mail body of invitaion mail.
   * Overrides signupMailBody so that we can use sendValidationEmail
   * in invite method. Otherwise, we would have to create a new method
   * that would be almost the same as sendValidationEmail.
   */
  def signupMailBody(user: TheUserType, validationLink: String, plainPassword: String): Elem = {
    //TODO Make the message to a resource.
    (<html>
        <head>
          <title>{S.??("sign.up.confirmation")}</title>
        </head>
        <body>
          <p>{S.??("dear")} {user.getFirstName},
            <br/>
            <br/>
            {S.??("sign.up.validation.link")}
            <br/><a href={validationLink}>{validationLink}</a>
            <br/>Your initial password is shown below:
            <br/> {plainPassword} 
            <br/>Please change it first time you log in.
            <br/>
            {S.??("thank.you")}
          </p>
        </body>
     </html>)
  }
  
  def sendValidationEmail(user: TheUserType, plainPassword: String) {
    val resetLink = S.hostAndPath+"/"+validateUserPath.mkString("/")+
    "/"+urlEncode(user.getUniqueId())

    val email: String = user.getEmail

    val msgXml = signupMailBody(user, resetLink, plainPassword)

    Mailer.sendMail(From(emailFrom),Subject(signupMailSubject),
                    (To(user.getEmail) ::
                     generateValidationEmailBodies(user, resetLink, plainPassword) :::
                     (bccEmail.toList.map(BCC(_)))) :_* )
  }

  protected def generateValidationEmailBodies(user: TheUserType,
                                              resetLink: String,
                                              plainPassword: String):
  List[MailBodyType] = List(xmlToMailBodyType(signupMailBody(user, resetLink, plainPassword)))

}

/**
 * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class User extends MegaProtoUser[User] {
  //TODO Move
  val MAX_NUM_PROJECTS = 1
  val CONF_MAX_NUM_PROJECTS = "limit.maxprojects"

  def getSingleton = User // what's the "meta" server

  //import net.liftweb.mapper.HoldsPlainWhenSet
  //override lazy val password =
  //  new MyPassword(this) with HoldsPlainWhenSet[User]
  override lazy val password = new MyPassword2(this)
  protected class MyPassword2(obj: User)
	    extends MappedPasswordWithPlainWhenSet(obj) {
    override def displayName = fieldOwner.passwordDisplayName
  }
 

  object sshKey extends MappedTextarea(this, 1024) {
    override def dbColumnName = "ssh_key"
    override def textareaRows  = 10
    override def textareaCols = 50
    override def displayName = "SSH public key"
  }

  object gitoriousUserId extends MappedInt(this) {
    override def dbColumnName = "gitorious_user_id"
    override def dbDisplay_? = false
  }

  /**
   *
   */
  def registerSshKey: Unit = {
    //GitosisHelper.createSshKey(this)
    //GitosisHelper.gitAddSshKey(this)
    //GitosisHelper.commitAndPush("Registered a new ssh key of the user " + id)

    //GitRepoManagerClient.removeSshKey(this)
    GitRepoManagerClient.addSshKey(this) match {
      case Full(v) => println("ssh_key id = " + v)
      case Empty => println ("empty??")
      case Failure(x, _, _) => println ("Failure: '" + x + "'")
    }
  }

  def changeGitoriousPassword: Unit = {
    //GitRepoManagerClient.addSshKey(this) match {
    //}
    //GitoriousHelper.changePassword(this)
  }

  /**
   * 
   */
  def maxNumProjects: Int = {
    tryo {
      val s = UserConfig.getValue(this, CONF_MAX_NUM_PROJECTS).get
      Integer.parseInt(s)
    } openOr {
      if(this.superUser_?) 10
      else MAX_NUM_PROJECTS
    }
  }
}

}
}
