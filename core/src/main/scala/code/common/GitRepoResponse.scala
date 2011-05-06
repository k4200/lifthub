package net.lifthub {
package common.event.gitrepo.response {

import net.liftweb.common.{Box, Failure }

import scala.reflect.BeanProperty

import net.lifthub.model._


trait GitRepoResponse


case class UserAdded(@BeanProperty result: Box[Int]) extends GitRepoResponse
case class SshKeyAdded(@BeanProperty result: Box[Int]) extends GitRepoResponse





}
}
