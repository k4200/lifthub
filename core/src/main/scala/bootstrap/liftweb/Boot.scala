package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import mapper._

import net.lifthub.model._
import net.lifthub.lib._


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = 
	new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
			     Props.get("db.url") openOr 
			     "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
			     Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)

      //Register sub-classes of DbHelper
      DbHelper.addDbHelper(DbType.MySql, MySqlHelper)
      //DbHelper.addDbHelper(DbType.PostgreSql, PostgreSqlHelper)

      DbHelper.all.foreach( helper => {
        helper.vendor match {
          case Full(vendor) =>
            LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)
            DB.defineConnectionManager(helper.connectionIdentifier, vendor)
          case Failure(x, _, _) => println(x)
          case Empty => println("helper.vendor is empty...")
        }
      })
    }

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, User)
    Schemifier.schemify(true, Schemifier.infoF _, Project)
    Schemifier.schemify(true, Schemifier.infoF _, UserDatabase)
    Schemifier.schemify(true, Schemifier.infoF _, ProjectTemplate)
    Schemifier.schemify(true, Schemifier.infoF _, UserConfig)

    // where to search snippet
    LiftRules.addToPackages("net.lifthub")

    val loggedIn = If(() => User.loggedIn_?,
                      () => RedirectResponse("/user_mgt/login"))

    val superUser_? = If(() => User.superUser_?,
                         () => RedirectResponse("/user_mgt/login"))

    // Build SiteMap
    def sitemap = SiteMap(
      List(
        Menu.i("Home") / "index" >> User.AddUserMenusAfter,
        Menu.i("Project Template") / "project_templates"
          submenus ProjectTemplate.menus 
          rule (superUser_?),
        Menu.i("Project") / "project" submenus (
          Project.menus
          ::: List(Menu.i("Operations") / "projects" / "operate")
        ) rule (loggedIn),
        Menu.i("Database") / "database" submenus UserDatabase.menus 
          rule (loggedIn),
        Menu.i("Log") / "log"  rule (loggedIn)
      ): _* )

      // more complex because this menu allows anything in the
      // /static path to be visible
//       Menu(Loc("Static", Link(List("static"), true, "/static/index"), 
// 	       "Static Content")))

    def sitemapMutators = User.sitemapMutator

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemapMutators(sitemap))

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

    //KK
    LiftRules.ajaxPostTimeout = 60000
    System.setProperty("mail.smtp.host", "localhost")

    // Add a query logger
//     val logger = Logger(classOf[Boot])
//     DB.addLogFunc {
//       case (log, duration) => {
//       logger.debug("Total query time : %d ms".format(duration))
//       log.allEntries.foreach {
// 	   case DBLogEntry(stmt,duration) =>
// 	     logger.debug("  %s in %d ms".format(stmt, duration))
//         }
//       }
//     }
  }
}
