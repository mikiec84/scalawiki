import sbt.Keys._
import Dependencies._

fork in Test in ThisBuild := true

lazy val commonSettings = Seq(
  organization := "org.scalawiki",
  version := "0.6.0",
  scalaVersion := Scala212V,
  crossScalaVersions := Seq(Scala212V, Scala211V),
  conflictManager := ConflictManager.strict,
  licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),

  Keys.resolvers ++= Dependencies.resolvers,

  libraryDependencies ++= Seq(
    Library.Specs2.core % Test,
    Library.Specs2.matcherExtra % Test,
    Library.Specs2.mock % Test,
    "com.google.jimfs" % "jimfs" % JimFsV % Test,
    "org.mock-server" % "mockserver-netty" % MockServerV % Test
  ),

  dependencyOverrides ++= Dependencies.overrides,

  initialize := {
    val _ = initialize.value
    val required = VersionNumber("1.8")
    val curr = VersionNumber(sys.props("java.specification.version"))
    assert(CompatibleJavaVersion(curr, required), s"Java $required or above required")
  },

  assemblyJarName in assembly := {
    s"${name.value}-${version.value}.jar"
  },
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case PathList("org", "xmlpull", "v1", xs@_*) => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val scalawiki = (project in file("."))
  .settings(commonSettings)
  .dependsOn(core, bots, dumps, wlx, sql, `http-extensions`)
  .aggregate(core, bots, dumps, wlx, sql, `http-extensions`)

lazy val core = Project("scalawiki-core", file("scalawiki-core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= {
    Seq(
      Library.Akka.actor,
      Library.Akka.stream,
      Library.Akka.http,
      Library.Akka.httpCaching,
      Library.Play.json,
      "com.typesafe" % "config" % TypesafeConfigV,
      "com.iheart" %% "ficus" % FicusV,
      "jp.ne.opt" %% "chronoscala" % ChronoScalaV,
      "ch.qos.logback" % "logback-classic" % LogbackClassicV,
      "org.sweble.wikitext" % "swc-engine" % SwcEngineV exclude("org.jsoup", "jsoup"),
      Library.Commons.codec,
      "org.jsoup" % "jsoup" % JSoupV,
      "com.softwaremill.retry" %% "retry" % RetryV,
      "net.openhft" % "chronicle-map" % ChronicleMapV,
      "com.concurrentthought.cla" %% "command-line-arguments" % CommandLineArgumentsV
    )
  }).dependsOn(`http-extensions`)

lazy val bots = Project("scalawiki-bots", file("scalawiki-bots"))
  .dependsOn(core % "compile->compile;test->test", wlx)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.github.pathikrit" %% "better-files-akka" % BetterFilesAkkaV,
    "com.concurrentthought.cla" %% "command-line-arguments" % CommandLineArgumentsV,
    "org.xwiki.commons" % "xwiki-commons-blame-api" % BlameApiV,
    Library.Poi.scratchpad,
    Library.Poi.ooxml,
    Library.Poi.converter,
    Library.Play.twirlApi,
    "com.github.tototoshi" %% "scala-csv" % ScalaCsvV
  ))
  .enablePlugins(SbtTwirl)

lazy val dumps = Project("scalawiki-dumps", file("scalawiki-dumps"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++=
    Seq("com.fasterxml" % "aalto-xml" % AaltoXmlV,
      Library.Commons.compress
    )
  )

lazy val wlx = Project("scalawiki-wlx", file("scalawiki-wlx"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.wookietreiber" %% "scala-chart" % ScalaChartV
    ),
    mainClass in assembly := Some("org.scalawiki.wlx.stat.Statistics")
  )

lazy val sql = Project("scalawiki-sql", file("scalawiki-sql"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    Library.Slick.slick,
    Library.Slick.hikaricp,
    "com.h2database" % "h2" % H2V
  ))

lazy val `http-extensions` = (project in file("http-extensions"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    Library.Akka.actor,
    Library.Akka.stream,
    Library.Akka.http,
    Library.Play.twirlApi,
    "org.scalacheck" %% "scalacheck" % ScalaCheckV % Test
  ))