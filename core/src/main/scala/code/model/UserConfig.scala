package net.lifthub {
package model {

import net.liftweb._
import mapper._
import common._


object UserConfig extends UserConfig with LongKeyedMetaMapper[UserConfig] {
  //override def fieldOrder = List(name, dateOfBirth, url)
  override def dbTableName = "user_config"

  def getValue(user: User, name: String): Box[String] = {
    find(By(UserConfig.user, user), By(UserConfig.name, name)).map(_.value)
  }
}

class UserConfig extends LongKeyedMapper[UserConfig] with IdPK {
  def getSingleton = UserConfig

  object user extends MappedLongForeignKey(this, User)
  object name extends MappedString(this, 40)
  object value extends MappedString(this, 100)
}


}
}
