import com.gu.deploy.PlayArtifact._
import sbt._
import sbt.Keys._
import play.Play.autoImport._
import PlayKeys._
import com.typesafe.sbt.web._

object UrlManagerBuild extends Build {

  val commonSettings =
    Seq(
      scalaVersion := "2.11.4",
      scalaVersion in ThisBuild := "2.11.4",
      organization := "com.gu",
      version      := "0.1",
      fork in Test := false,
      resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
      scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
      doc in Compile <<= target.map(_ / "none"),
      incOptions := incOptions.value.withNameHashing(nameHashing = true)
    )

  val root = Project("url-manager", file(".")).enablePlugins(play.PlayScala).enablePlugins(SbtWeb)
    .settings(libraryDependencies ++= Seq(
      ws,
      "com.amazonaws" % "aws-java-sdk" % "1.9.23")
    ).settings(commonSettings ++ playArtifactDistSettings ++ playArtifactSettings: _*)
    .settings(magentaPackageName := "url-manager")

  def playArtifactSettings = Seq(
    ivyXML :=
      <dependencies>
        <exclude org="commons-logging"/>
        <exclude org="org.springframework"/>
        <exclude org="org.scala-tools.sbt"/>
      </dependencies>
  )
}
