package net.lifthub {
package lib {

import org.specs._

object ProjectHelperSpec extends Specification {
//   "'foo' is a three-letter word." in {
//     "foo".size must_== 3
//   }
  "TemplateType" should {
    "contain correct dirName" in {
      TemplateType.Mvc.dirName mustEqual "lift_mvc"
    }
  }

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
      ProjectInfo.gitosisAdminPath mustEqual "/home/lifthub/gitosis-admin"
    }
  }

  "ProjectHelper" should {
    val pi = ProjectInfo("foo", TemplateType.Mvc, "2.2")
    "copy template" in {
      ProjectHelper.copyTemplate(pi) mustEqual
	"cp /home/lifthub/projecttemplates/lift_2.2_sbt/lift_mvc /home/lifthub/userprojects/foo"
    }
  }
}

}
}
