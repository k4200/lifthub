package net.lifthub {
package common.event.gitrepo.response {

import net.liftweb.common.{Box, Failure }

import scala.reflect.BeanProperty

import net.lifthub.model._


trait GitRepoResponse


case class ResAddUser(@BeanProperty result: Box[Int]) extends GitRepoResponse
case class ResAddSshKey(@BeanProperty result: Box[Int]) extends GitRepoResponse





}
}
