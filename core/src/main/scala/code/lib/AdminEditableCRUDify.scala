package net.lifthub {
package lib {

import net.liftweb._
import common._;
import http._;
import mapper._
import sitemap._;
import Loc._;
import proto.{ProtoUser => GenProtoUser}
//import util.Helpers.tryo


trait AdminEditableCRUDify [KeyType,
                            CrudType <: AdminEditableKeyedMapper[KeyType, CrudType]]
extends MegaCRUDify [KeyType, CrudType] {
  self: CrudType with KeyedMetaMapper[KeyType, CrudType] =>
  val superUser_? = If(() => self.userObject.superUser_? ,
                       () => RedirectResponse("/user_mgt/login"));

  override def showAllMenuLocParams: List[Loc.AnyLocParam] = List(superUser_?);
  override def viewMenuLocParams: List[Loc.AnyLocParam] = List(superUser_?)
  override def createMenuLocParams: List[Loc.AnyLocParam] = List(superUser_?);
  override def editMenuLocParams: List[Loc.AnyLocParam] = List(superUser_?);
  override def deleteMenuLocParams: List[Loc.AnyLocParam] = List(superUser_?);
  //override def deleteMenuLoc: Box[Menu] = Empty;

}


trait AdminEditableKeyedMapper[KeyType, OwnerType <: KeyedMapper[KeyType, OwnerType]]
extends KeyedMapper[KeyType, OwnerType] {
  self: OwnerType =>

  val userObject: GenProtoUser

}



}
}
