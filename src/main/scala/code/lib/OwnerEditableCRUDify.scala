package net.lifthub {
package lib {


import net.liftweb._
import common._;
import http._;
import mapper._
import sitemap._;
import Loc._;
import proto.{ProtoUser => GenProtoUser}

trait UserEditableCRUDify [KeyType,
                           CrudType <: UserEditableKeyedMapper[KeyType, CrudType]]
//TODO UserType <: ProtoUser[UserType] ?
extends CRUDify [KeyType, CrudType] {
  self: CrudType with KeyedMetaMapper[KeyType, CrudType] =>

  override protected def doDeleteSubmit(item: TheCrudType, from: String)() = {
    item.userObject.currentUserId match {
      case Full(userId) => 
        if(userId == item.userId.is) {
	  S.notice(S ? "Deleted")
	  item.delete_!
	}
        S.redirectTo(from)
      case _ =>  S.redirectTo("/user_mgt/login")
    }
  }
}

trait UserEditableKeyedMapper[KeyType, OwnerType <: KeyedMapper[KeyType, OwnerType]]
//extends LongKeyedMapper[OwnerType] {
extends KeyedMapper[KeyType, OwnerType] {
  self: OwnerType =>
  //type UserType = ProtoUser[GenProtoUser]

  //import net.lifthub.model.User
  //type UserType = MegaProtoUser[User]
  //type UserType = MetaMegaProtoUser[User]
  type UserType <: KeyedMapper[Long,UserType]
  val userObject: GenProtoUser
  //def userMeta: KeyedMetaMapper[Long, GenProtoUser] = userObject.asInstanceOf[KeyedMetaMapper[Long, GenProtoUser]]
  def userMeta: KeyedMetaMapper[Long, UserType]
  //def userMeta: KeyedMetaMapper[Long, GenProtoUser]

  //ProtoUser[GenProtoUser]
  lazy val user: MappedLongForeignKey[OwnerType, KeyedMetaMapper[Long, UserType]]
  //lazy val user: MappedLongForeignKey[OwnerType, KeyedMetaMapper[Long, GenProtoUser]]
    = new MyUserField(this, userMeta)
  //lazy val user: MappedLongForeignKey[OwnerType, ProtoUser[GenProtoUser]]
  //  = new MyUserField(this, userObject.asInstanceOf[KeyedMetaMapper[Long, GenProtoUser]])

  //protected class MyUserField[T<:Mapper[T],O<:KeyedMapper[Long, O]](obj: OwnerType, _foreignMeta: KeyedMetaMapper[Long, UserType]) 
  protected class MyUserField[OwnerType, UserType](obj: OwnerType, _foreignMeta: KeyedMetaMapper[Long, UserType]) 
            extends MappedLongForeignKey(obj, _foreignMeta)


  lazy val userId: MappedLong[OwnerType] = new MyUserId(this)
  protected class MyUserId(obj: OwnerType) extends MappedLong(obj)
  //This doesn't compile.
  //object userId extends MappedLong(this)

}

// trait OwnerUser {
//   self: BaseLongKeyedMapper =>
//   def userIdField = id
//   object id extends MappedLongIndex[MapperType](this.asInstanceOf[MapperType])
// }




}
}
