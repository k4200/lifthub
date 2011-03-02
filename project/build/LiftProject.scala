import sbt._

/**
 * Main project class.
 */
class LifthubProject(info: ProjectInfo) extends ParentProject(info) {
  // def project(path: Path, name: String, deps: Project*): Project
  lazy val core = project("core", "Lifthub core", new CoreProject(_))
  lazy val web = project("web", "Lifthub web", new LiftProject(_), core)
  lazy val bgtasks = project("bgtasks", "Lifthub bg tasks", new BgProject(_), core)

  override def parallelExecution = true
  
}

protected class CoreProject(info: ProjectInfo) extends DefaultProject(info)
with AkkaProject {
  val liftVersion = "2.2"

  val jGitRepo = "JGit" at "http://download.eclipse.org/jgit/maven"
  override def libraryDependencies = Set(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-mapper" % liftVersion % "compile->default",
    "org.mortbay.jetty" % "jetty" % "6.1.26" % "test->default",
    "junit" % "junit" % "4.5" % "test->default",
    "ch.qos.logback" % "logback-classic" % "0.9.26",
    "org.scala-tools.testing" %% "specs" % "1.6.6" % "test->default",
    "org.mockito" % "mockito-core" % "1.8.5" % "test->default",
    "mysql" % "mysql-connector-java" % "5.1.14", //MySQL
    "commons-io" % "commons-io" % "2.0.1",
    "org.eclipse.jgit" % "org.eclipse.jgit" % "0.10.1" withJavadoc,
    "commons-lang" % "commons-lang" % "2.5",
    "org.apache.commons" % "commons-exec" % "1.1"
    //"org.scala-tools.sbt" % "sbt-launch" % "0.7.2" // doesn't work with 2.8
    // https://github.com/harrah/process
    //"org.scala-tools.sbt" % "process" % "0.1" // doesn't work with 2.8
    //"org.eclipse.jgit" % "org.eclipse.jgit" % "0.10.1" sources
    //"com.h2database" % "h2" % "1.2.138"
  ) ++ super.libraryDependencies

  val akkaRemote = akkaModule("remote")
}

protected class LiftProject(info: ProjectInfo)
extends DefaultWebProject(info) {
  override def artifactID = "lifthub"

  // uncomment the following if you want to use the snapshot repo
  // val scalatoolsSnapshot = ScalaToolsSnapshots

  // If you're using JRebel for Lift development, uncomment
  // this line
  // override def scanDirectories = Nil
}

protected class BgProject(info: ProjectInfo) extends DefaultProject(info)
with AkkaProject {
  override def libraryDependencies = Set(
    //"commons-daemon" % "commons-daemon" % "1.0.3",
    //"org.apache.commons" % "commons-daemon" % "1.0.3",
    //"org.mortbay.jetty" % "jetty" % "6.1.26"
  ) ++ super.libraryDependencies
}
