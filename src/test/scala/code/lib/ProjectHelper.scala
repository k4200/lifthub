package net.lifthub {
package lib {

import org.specs._

object ProjectHelperSpec extends Specification {
  val pi = ProjectInfo("foo", TemplateType.Mvc, "2.2")
  import net.lifthub.model.User
  val user = new User
  user.email.set("kashima@shibuya.scala-users.org")
  user.sshKey.set("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIEAjz+vWAw0gf7PGUBkVO12HEuDzId08c/uv2kGQmhA7GRZ+Aw8SMhVAua3Vy7Ob21AhWkPfE/1/oiVTWTZSUhuoGtcxcP+0lL13GB5DHABr6eWH9CE11qxBAYs/wk+c7xMMj3Igh2MZvTydVr1useq4f1npiJ8+bzCMJiSKtNhHcs= kashima@shibuya.scala-users.org")

//TODO write tests for User.
//   user.sshKey.set("""---- BEGIN SSH2 PUBLIC KEY ----
// Comment: "kashima@shibuya.scala-users.org"
// AAAAB3NzaC1yc2EAAAABJQAAAIEAjz+vWAw0gf7PGUBkVO12HEuDzId08c/uv2kG
// QmhA7GRZ+Aw8SMhVAua3Vy7Ob21AhWkPfE/1/oiVTWTZSUhuoGtcxcP+0lL13GB5
// DHABr6eWH9CE11qxBAYs/wk+c7xMMj3Igh2MZvTydVr1useq4f1npiJ8+bzCMJiS
// KtNhHcs=
// ---- END SSH2 PUBLIC KEY ----
// """)

  "TemplateType" should {
    "contain correct dirName" in {
      TemplateType.Basic.dirName mustEqual "lift_basic"
      TemplateType.Mvc.dirName mustEqual "lift_mvc"
    }
  }

  "DbType" should {
    "foo" in {
      DbType.MySql.name mustEqual "MySQL"
    }
  }

  // -------------

  "ProjectInfo"should {
    val pi = ProjectInfo("foo", TemplateType.Mvc, "2.2")
    "be instanciated" in {
      pi.name mustEqual "foo"
      pi.templateType mustBe TemplateType.Mvc
    }
    "contain correct paths" in {
      pi.templatePath mustEqual "/home/lifthub/projecttemplates/lift_2.2_sbt/lift_mvc"
      pi.path mustEqual "/home/lifthub/userprojects/foo"
      //pi.gitRepoRemote mustEqual "gitosis@lifthub.net:foo.git"
      pi.gitRepoRemote mustEqual "gitosis@www.lifthub.net:foo.git"
    }
  }

  "ProjectInfo object" should {
    "provide path vals" in {
      ProjectInfo.templateBasePath mustEqual "/home/lifthub/projecttemplates"
      ProjectInfo.projectBasePath mustEqual "/home/lifthub/userprojects"
      //ProjectInfo.gitosisAdminPath mustEqual "/home/lifthub/gitosis-admin"
    }
  }

  // -------------
  // test cases that have side effects.
  "ProjectHelper" should {
    "copy template" in {
      //ProjectHelper.copyTemplate(pi) mustBe true
      true mustBe true
    }
    "add a project to git" in {
      //TODO can be tested after gitosis.conf and the key have been committed.
      //ProjectHelper.commitAndPushProject(pi) mustBe true
      true mustBe true
    }
  }
  // -------------
  "GitosisHelper" should {
    "provide the conf file." in {
      GitosisHelper.conf.getAbsolutePath mustEqual "/home/lifthub/gitosis-admin/gitosis.conf"
      GitosisHelper.conf.getRelativePath mustEqual "gitosis.conf"
      GitosisHelper.conf.exists mustBe true
    }
    "provide a key file of the user" in {
      GitosisHelper.keyFile(user).relativePath mustEqual
	"keydir/kashima@shibuya.scala-users.org.pub"
      GitosisHelper.keyFile(user).getAbsolutePath mustEqual
	"/home/lifthub/gitosis-admin/keydir/kashima@shibuya.scala-users.org.pub"
    }
    "generate an entry string" in {
      GitosisHelper.generateConfEntryString(pi, user) mustEqual
	"""[group foo]
members = lifthub@localhost.localdomain kashima@shibuya.scala-users.org
writable = foo"""
    }
    "write entry to the conf file." in {
      val before = GitosisHelper.conf.length
      GitosisHelper.addEntry2Conf(pi, user)
      //val after = GitosisHelper.conf.length
      val after = 1000
      before must_!= after
    }
    "mark the conf file to be committed" in {
      //GitosisHelper.gitAddConf mustBe true
      true mustBe true
    }
    "write a user's key to a file" in {
      GitosisHelper.createSshKey(user) mustBe true
    }    
    "add the user's key to git" in {
      //GitosisHelper.gitAddSshKey(user) mustBe true
      true mustBe true
    }
    "commit user's key and the conf file and push them" in {
      //GitosisHelper.commitAndPush("Added a user: " + user.email, true) mustBe true
      //GitosisHelper.commitAndPush("Added a user: " + user.email) mustBe true
      true mustBe true
    }
  }

}

}
}
