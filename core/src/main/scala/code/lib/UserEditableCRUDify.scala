package net.lifthub {
package lib {


import net.liftweb._
import common._;
import http._;
import mapper._
import sitemap._;
import Loc._;
import proto.{ProtoUser => GenProtoUser}
import util.Helpers.tryo

trait UserEditableCRUDify [KeyType,
                           CrudType <: UserEditableKeyedMapper[KeyType, CrudType]]
//TODO UserType <: ProtoUser[UserType] ?
extends MegaCRUDify [KeyType, CrudType] {
  self: CrudType with KeyedMetaMapper[KeyType, CrudType] =>

  //TODO Implement the other actions.
  //TODO Write test cases.

  override protected def doDeleteSubmit(item: TheCrudType, from: String)() = {
    self.userObject.currentUserId match {
      case Full(userId) => 
        if(tryo{userId.toLong}.getOrElse(-1) == item.userId.is) {
	  S.notice(S ? "Deleted")
	  item.delete_!
	} else {
	  println("You (user ID %s) are not the item %d's owner."
                  .format(userId, item.userId.is))
	}
        S.redirectTo(from)
      case _ =>  S.redirectTo("/user_mgt/login")
    }
  }

  val loggedIn_? = If(() => self.userObject.loggedIn_? ,
                      () => RedirectResponse("/user_mgt/login"))

  override def showAllMenuLocParams: List[Loc.AnyLocParam] = List(loggedIn_?);
  override def viewMenuLocParams: List[Loc.AnyLocParam] = List(loggedIn_?)
  override def createMenuLocParams: List[Loc.AnyLocParam] = List(loggedIn_?);
  override def editMenuLocParams: List[Loc.AnyLocParam] = List(loggedIn_?);
  override def deleteMenuLocParams: List[Loc.AnyLocParam] = List(loggedIn_?);

  override def findForListParams: List[QueryParam[CrudType]] =
    ((for(userIdStr <- self.userObject.currentUserId;
         userId <- tryo{userIdStr.toInt})
    yield
      List(By(self.userId, userId))
    ) openOr Nil) ::: super.findForListParams

  /**
   * Finds all the records of the current logged-in user.
   */
  def findAllByCurrentUser: List[TheCrudType] = 
    findAll(findForListParams: _*)

  /**
   * Finds a record of the current logged-in user that matches 
   * the given query parameters.
   */
  def findByCurrentUser(by: QueryParam[TheCrudType]): List[TheCrudType] =
    findAll((by :: findForListParams): _*)
  def findByCurrentUser(by: List[QueryParam[TheCrudType]]): List[TheCrudType] =
    findAll((by ::: findForListParams): _*)
}

trait UserEditableKeyedMapper[KeyType, OwnerType <: KeyedMapper[KeyType, OwnerType]]
//extends LongKeyedMapper[OwnerType] {
extends KeyedMapper[KeyType, OwnerType] {
  self: OwnerType =>

  val userObject: GenProtoUser
  lazy val userId: MappedLong[OwnerType] = new MyUserId(this)
  protected class MyUserId(obj: OwnerType) extends MappedLong(obj) {
    override def dbIndexed_? = true
    override def dbDisplay_? = false

  }
  //This doesn't compile.
  //object userId extends MappedLong(this)

}



}
}
