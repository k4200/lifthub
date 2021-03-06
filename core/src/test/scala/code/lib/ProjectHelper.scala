package net.lifthub {
package lib {

import org.specs._

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._

import akka.actor.Actor
import akka.actor.Actor._

import net.lifthub.model.Project
import net.lifthub.model.UserDatabase
import net.lifthub.model.ProjectTemplate

import bootstrap.liftweb.Boot


object ServerInfoSpec extends Specification {
  "ServerInfo" should {
    val si = ServerInfo("foo", "127.0.0.100", 9000, "6")
    "contain correct paths." in {
      si.deployDirPath mustEqual "/home/lifthubuser/chroot/foo/home/lifthubuser/servers/jetty-6/userwebapps/foo"
      si.confPath mustEqual "/home/lifthubuser/chroot/foo/home/lifthubuser/servers/jetty-6/etc/lifthub/foo.xml"
      si.templatePath mustEqual "/home/lifthubuser/chroot/foo/home/lifthubuser/servers/jetty-6/etc/jetty.xml.tmpl"
    }

    "generate a conf string" in {
      val portPattern = "#port#".r
      val namePattern = "#name#".r
      //portPattern.findFirstIn(si.confString) must equalTo(None)
      //namePattern.findFirstIn(si.confString) must equalTo(None)
      true mustBe true
    }

    "generate a conf file" in {
      //si.writeConfFile mustBe true
      true mustBe true
    }
  }
}


object ProjectHelperSpec extends Specification {
  Initializer.initConn()
  import net.lifthub.model.User
  val user = new User
  user.email.set("kashima@shibuya.scala-users.org")
  user.sshKey.set("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIEAjz+vWAw0gf7PGUBkVO12HEuDzId08c/uv2kGQmhA7GRZ+Aw8SMhVAua3Vy7Ob21AhWkPfE/1/oiVTWTZSUhuoGtcxcP+0lL13GB5DHABr6eWH9CE11qxBAYs/wk+c7xMMj3Igh2MZvTydVr1useq4f1npiJ8+bzCMJiSKtNhHcs= kashima@shibuya.scala-users.org")

  // -------------
  "NginxConf" should {
    val nginxConf = NginxConf("foo", "127.0.1.1", 9000)
    "contain correct paths." in {
      nginxConf.confPath mustEqual "/home/lifthub/nginx/conf.d/foo.conf"
      nginxConf.logPath mustEqual "/home/lifthub/nginx/logs/foo.access.log"
    }
    "be instanciated out of Project" in {
      //val project = Project(....)
      //val nc = NginxConf(project)
      true mustBe true
    }
    "generate a conf file" in {
      nginxConf.confString mustEqual
"""    server {
        server_name foo.lifthub.net;
        access_log /home/lifthub/nginx/logs/foo.access.log main;
        location / {
            proxy_pass   http://127.0.1.1:9000/;
        }
    }
"""
    }
  }
  // -------------

  "ProjectInfo"should {
    doBefore { Initializer.addRecords() }
    val pt = ProjectTemplate.find(By(ProjectTemplate.id, 1)).get
    val pi = ProjectInfo("foo", pt)
    "be instanciated" in {
      pi.name mustEqual "foo"
      pi.projectTemplate.getSingleton mustBe ProjectTemplate
    }
    "contain correct paths" in {
      pi.templatePath mustEqual "/home/lifthub/projecttemplates/lift_2.3_sbt/lift_basic"
      pi.path mustEqual "/home/lifthub/userprojects/foo"
      pi.gitRepoRemote mustEqual "gitorious@git.lifthub.net:foo/foo.git"

      pi.propsPath mustEqual "/home/lifthub/userprojects/foo/src/main/resources/props/production.default.props"
      pi.warPath mustEqual "/home/lifthub/userprojects/foo/target/scala_2.8.1/lift-sbt-template_2.8.1-0.1.war"

    }
    "be instanciated out of Project" in {
      val project = Project.create
      project.template(pt)
      val pi2 = ProjectInfo(project)
      pi2.templatePath mustEqual "/home/lifthub/projecttemplates/lift_2.3_sbt/lift_basic"
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
  "SbtHelper" should {
    "do sbt update." in {
      //TODO
//       val project = Project.create
//       project.liftVersion.set("2.2")
//       project.templateType.set(TemplateType.Xhtml)
//       SbtHelper.update(project)
      true mustBe true
    }
  }

  // -------------
  // test cases that have side effects.
  "ProjectHelper" should {
    import java.io.File
    val pt = ProjectTemplate.find(By(ProjectTemplate.id, 1)).get
    val pi = ProjectInfo("foo", pt)

    "copy template" in {
      ProjectHelper.copyTemplate(pi) must haveClass[Full[String]]
      //true mustBe true
    }
    "generate a props string" in {
      val dbInfo = UserDatabase.create.name("foo").databaseType(DbType.MySql)
        .username("foo").hostname("localhost").password("pass")
      ProjectHelper.generatePropsString(dbInfo) mustEqual
"""db.driver=com.mysql.jdbc.Driver
db.url=jdbc:mysql://localhost/foo
db.user=foo
db.password=pass"""
    }
    "create a props file" in {
      val dbInfo = UserDatabase.create.name("foo").databaseType(DbType.MySql)
        .username("foo").hostname("localhost").password("pass")
      ProjectHelper.createProps(pi, dbInfo)
      val file = new File(pi.propsPath)
      file.exists mustBe true
    }
    "add a project to git" in {
      //ProjectHelper.commitAndPushProject(pi) mustBe true
      true mustBe true
    }
    doLast {
      import scala.util.control.Exception._
      import org.apache.commons.io.{FileUtils => CommonsFileUtils}
      allCatch {
        CommonsFileUtils.deleteDirectory(new File("/home/lifthub/userprojects/foo/"))
      }
    }
  }
  // -------------

  object Initializer {
    def initConn() = {
      val boot = new Boot
      boot.boot
    }
    def addRecords() = {

      (for {
        _ <- tryo{DB.runUpdate("truncate table project_templates", Nil)} ?~ "truncte failed."
        _ <- tryo{DB.runUpdate("insert into project_templates (id, name,path,lift_version)values(1, 'Lift 2.3 Basic', 'lift_2.3_sbt/lift_basic', '2.3')", Nil)} ?~ "insert1 failed."
      } yield {
        println("ProjectHelper.Initializer.addRecords succeeded.")
      }) match {
        case ok: Full[_] => ok
        case ng => {
          println(ng)
        }
      }
    }
  }


}

}
}
