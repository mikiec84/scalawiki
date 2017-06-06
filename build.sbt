import sbt.Keys._

val akkaV = "2.4.18"
val sprayV = "1.3.4"
val specsV = "3.7.2"
val scalajsReactV = ""

lazy val commonSettings = Seq(
  organization := "org.scalawiki",
  version := "0.5-M5",
  scalaVersion := "2.11.11",

  libraryDependencies ++= Seq(
    "org.specs2" %% "specs2-core" % specsV % Test,
    "org.specs2" %% "specs2-matcher-extra" % specsV % Test,
    "org.specs2" % "specs2-mock_2.11" % specsV % Test,
    "com.google.jimfs" % "jimfs" % "1.1" % Test
  ),

  resolvers := Seq("spray repo" at "http://repo.spray.io",
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("rick-beton", "maven")
  ),
  scalacOptions ++= Seq("-Ybackend:GenBCode"),

  initialize := {
    val _ = initialize.value
    // run the previous initialization
    val required = VersionNumber("1.8")
    val curr = VersionNumber(sys.props("java.specification.version"))
    assert(CompatibleJavaVersion(curr, required), s"Java $required or above required")
  }
)

lazy val scalawiki =
  (project in file("."))
    .settings(commonSettings)
    .dependsOn(
      `scalawiki-core`, `scalawiki-bots`, `scalawiki-dumps`, `scalawiki-wlx`, `scalawiki-sql`,
      `http-extensions`, `scalawiki-web-ui-server`, `scalawiki-web-ui-client`
    )
    .aggregate(
      `scalawiki-core`, `scalawiki-bots`, `scalawiki-dumps`, `scalawiki-wlx`, `scalawiki-sql`,
      `http-extensions`, `scalawiki-web-ui-server`, `scalawiki-web-ui-client`
    )

lazy val `scalawiki-core` =
  (project in file("scalawiki-core"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= {
      Seq(
        "io.spray" %% "spray-util" % sprayV,
        "io.spray" %% "spray-caching" % sprayV,
        "com.typesafe.play" %% "play-json" % "2.5.12",
        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "com.typesafe.akka" %% "akka-http" % "10.0.6",
        "com.typesafe" % "config" % "1.3.0",
        "com.iheart" %% "ficus" % "1.2.3",
        "com.github.nscala-time" %% "nscala-time" % "2.10.0",
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "org.sweble.wikitext" % "swc-engine" % "2.0.0" exclude("org.jsoup", "jsoup"),
        "commons-codec" % "commons-codec" % "1.10",
        "org.jsoup" % "jsoup" % "1.8.3"
      )
    }).dependsOn(`http-extensions`)

lazy val `scalawiki-bots` =
  (project in file("scalawiki-bots"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files-akka" % "2.15.0",
      "com.concurrentthought.cla" %% "command-line-arguments" % "0.3.0",
      "org.xwiki.commons" % "xwiki-commons-blame-api" % "6.4.1",
      "org.apache.poi" % "poi-scratchpad" % "3.13",
      "org.apache.poi" % "poi-ooxml" % "3.13",
      "fr.opensagres.xdocreport" % "org.apache.poi.xwpf.converter.xhtml" % "1.0.5",
      "com.typesafe.play" % "twirl-api_2.11" % "1.3.0",
      "com.github.tototoshi" %% "scala-csv" % "1.3.4"
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test", `scalawiki-wlx`)
    .enablePlugins(SbtTwirl)

lazy val `scalawiki-dumps` =
  (project in file("scalawiki-dumps"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++=
      Seq("com.fasterxml" % "aalto-xml" % "1.0.0",
        //    "org.codehaus.woodstox" % "woodstox-core-asl" % "4.4.1",
        "org.apache.commons" % "commons-compress" % "1.9")
    )
    .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `scalawiki-wlx` =
  (project in file("scalawiki-wlx"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.wookietreiber" %% "scala-chart" % "0.5.0",
      "com.concurrentthought.cla" %% "command-line-arguments" % "0.3.0"
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `scalawiki-sql` =
  (project in file("scalawiki-sql"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.1.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
      "com.h2database" % "h2" % "1.4.189"
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `http-extensions` =
  (project in file("http-extensions"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.0.6",
      "com.typesafe.akka" %% "akka-actor" % akkaV,
      "com.typesafe.play" %% "twirl-api" % "1.3.0",
      "org.scalacheck" %% "scalacheck" % "1.11.3" % Test
    ))

lazy val `scalawiki-web-ui-server` =
  (project in file("scalawiki-web-ui/server"))
    .settings(commonSettings: _*)
    .dependsOn(`scalawiki-core` % "compile->compile;test->test", `scalawiki-wlx`)
    .enablePlugins(SbtTwirl)

lazy val `scalawiki-web-ui-client` =
  (project in file("scalawiki-web-ui/client"))
    .settings(commonSettings: _*)
    .settings(scalaJSUseMainModuleInitializer := true)
    .dependsOn(`scalawiki-core` % "compile->compile;test->test", `scalawiki-wlx`)
    .enablePlugins(ScalaJSPlugin)



/** Dependencies only used by the JS project (note the use of %%% instead of %%) */
val scalajsDependencies = Def.setting(Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % versions.scalajsReact,
  "com.github.japgolly.scalajs-react" %%% "extra" % versions.scalajsReact,
  "com.github.japgolly.scalacss" %%% "ext-react" % versions.scalaCSS,
  "me.chrons" %%% "diode" % versions.diode,
  "me.chrons" %%% "diode-react" % versions.diode,
  "org.scala-js" %%% "scalajs-dom" % versions.scalaDom,
  "com.lihaoyi" %%% "utest" % versions.uTest % Test
))

/** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
val jsDependencies = Def.setting(Seq(
  "org.webjars.bower" % "react" % versions.react / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
  "org.webjars.bower" % "react" % versions.react / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
  "org.webjars" % "jquery" % versions.jQuery / "jquery.js" minified "jquery.min.js",
  "org.webjars" % "bootstrap" % versions.bootstrap / "bootstrap.js" minified "bootstrap.min.js" dependsOn "jquery.js",
  "org.webjars" % "chartjs" % versions.chartjs / "Chart.js" minified "Chart.min.js",
  "org.webjars" % "log4javascript" % versions.log4js / "js/log4javascript_uncompressed.js" minified "js/log4javascript.js"
))
