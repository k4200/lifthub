package net.lifthub {
package lib {

import org.specs._

object ProjectHelperSpec extends Specification {
  val pi = ProjectInfo("foo", TemplateType.Mvc, "2.2")
  import net.lifthub.model.User
  val user = new User
  user.email.set("kashima@shibuya.scala-users.org")
  user.sshKey.set("""---- BEGIN SSH2 PUBLIC KEY ----
Comment: "kashima@shibuya.scala-users.org"
AAAAB3NzaC1yc2EAAAABJQAAAIEAjz+vWAw0gf7PGUBkVO12HEuDzId08c/uv2kG
QmhA7GRZ+Aw8SMhVAua3Vy7Ob21AhWkPfE/1/oiVTWTZSUhuoGtcxcP+0lL13GB5
DHABr6eWH9CE11qxBAYs/wk+c7xMMj3Igh2MZvTydVr1useq4f1npiJ8+bzCMJiS
KtNhHcs=
---- END SSH2 PUBLIC KEY ----
""")

  "TemplateType" should {
    "contain correct dirName" in {
      TemplateType.Mvc.dirName mustEqual "lift_mvc"
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

  "ProjectHelper" should {
    "copy template" in {
       ProjectHelper.copyTemplate(pi) mustBe true
//       ProjectHelper.copyTemplate(pi) mustEqual
// 	"cp /home/lifthub/projecttemplates/lift_2.2_sbt/lift_mvc /home/lifthub/userprojects/foo"
    }
    
  }
  // -------------
  "GitosisHelper" should {
    "provide the conf file." in {
      GitosisHelper.conf.getAbsolutePath mustEqual "/home/lifthub/gitosis-admin/gitosis.conf"
      GitosisHelper.conf.exists mustBe true
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
      val after = GitosisHelper.conf.length
      before must_!= after
    }
    "write a user's key to a file" in {
      GitosisHelper.createSshKey(user) mustBe true
    }

  }

}

}
}
