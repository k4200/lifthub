package net.lifthub {
package common.event.gitrepo.response {

import net.liftweb.common.{Box, Failure}


import net.lifthub.model._


trait GitRepoResponse


case class ResAddUser(result: Box[Int]) extends GitRepoResponse
case class ResRemoveUser(result: Box[Int]) extends GitRepoResponse
case class ResAddSshKey(result: Box[Int]) extends GitRepoResponse
case class ResRemoveSshKey(result: Box[Int]) extends GitRepoResponse
case class ResAddProject(result: Box[Int]) extends GitRepoResponse
case class ResRemoveProject(result: Box[Int]) extends GitRepoResponse





}
}
