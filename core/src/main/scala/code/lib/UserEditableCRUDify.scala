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
extends CRUDify [KeyType, CrudType] {
  self: CrudType with KeyedMetaMapper[KeyType, CrudType] =>

  //TODO Implement the other actions.

  override protected def doDeleteSubmit(item: TheCrudType, from: String)() = {
    self.userObject.currentUserId match {
      case Full(userId) => 
        if(userId == item.userId.is) {
	  S.notice(S ? "Deleted")
	  item.delete_!
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

  def findAllOfOwner: List[TheCrudType] = {
    (for(userIdStr <- self.userObject.currentUserId;
         userId <- tryo{userIdStr.toInt})
    yield
      findAll(By(self.userId, userId))
    ) openOr Nil
  }
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