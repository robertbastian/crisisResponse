import com.earldouglas.xwp.JettyPlugin
import com.mojolly.scalate.ScalatePlugin.ScalateKeys._
import com.mojolly.scalate.ScalatePlugin._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import org.scalatra.sbt._
import sbt.Keys._
import sbt._

object CrisisResponseSystemBuild extends Build {
  val Organization = "de.rb"
  val Name = "Crisis Response System"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.7"
  val ScalatraVersion = "2.4.0"


  lazy val project = Project (
    "crisis-response-system",
    file("."),
    settings = ScalatraPlugin.scalatraSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      scalacOptions ++= Seq("-unchecked", "-deprecation","-feature"),
      libraryDependencies ++= Seq(
        // Scalatra
        "org.scalatra" %% "scalatra" % "latest.integration",
        // Tests
        "org.scalatra" %% "scalatra-specs2" % "latest.integration" % "test",
        "org.specs2" % "specs2_2.11" % "3.7" % "test",
        // Container
        "org.eclipse.jetty" % "jetty-webapp" % "latest.integration" % "compile;container",
//        "org.eclipse.jetty" % "jetty-plus" % "latest.integration" % "compile;container",
        "javax.servlet" % "javax.servlet-api" % "latest.integration" % "provided",
        // JSON
        "org.scalatra" %% "scalatra-json" % "latest.integration",
        "org.json4s" % "json4s-jackson_2.11" % "3.3.0",
        // DB
        "com.typesafe.slick" %% "slick" % "latest.integration",
        "mysql" % "mysql-connector-java" % "latest.integration",
        "com.mchange" % "c3p0" % "latest.integration",
        // Twitter
        "org.twitter4j" % "twitter4j-stream" % "latest.integration",
        //CSV
        "com.github.marklister" %% "product-collections" % "1.4.2"
      )
    )
  ).enablePlugins(JettyPlugin,JavaAppPackaging)
}
