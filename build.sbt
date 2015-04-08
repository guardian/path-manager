addCommandAlias("dist", ";riffRaffArtifact")

import com.typesafe.sbt.packager.Keys._
//import sbtassembly.Plugin.AssemblyKeys._
import play.PlayImport.PlayKeys._

name := "path-manager"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

version := "1.0"


lazy val root = project.in(file("."))
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(
    // Never interested in the version number in the artifact name
    name in Universal := normalizedName.value,
    riffRaffArtifactPublishPath := normalizedName.value,
    scalaVersion := "2.11.4",
    scalaVersion in ThisBuild := "2.11.4",
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
    doc in Compile <<= target.map(_ / "none"),
    fork in Test := false
  ).aggregate(pathManager, migrator)

lazy val pathManager = project.in(file("path-manager"))
  .enablePlugins(PlayScala, RiffRaffArtifact)
  .settings(
    name := "path-manager",
    name in Universal := normalizedName.value,
    playDefaultPort := 10000,
    //riffRaffPackageType := (dist in Universal).value,
    riffRaffPackageType := (packageZipTarball in config("universal")).value,
    libraryDependencies ++= Seq(
      ws,
      "com.amazonaws" % "aws-java-sdk" % "1.9.23",
      "org.apache.commons" % "commons-lang3" % "3.3.2",
      "net.logstash.logback" % "logstash-logback-encoder" % "4.2",
      "org.scalatestplus" %% "play" % "1.1.0" % "test"
    )
  )

lazy val migrator = project.in(file("migrator"))
  .enablePlugins(RiffRaffArtifact)
  .settings(
    name := "migrator",
    mainClass in assembly := Some("com.gu.pathmanager.Migrator"),
    assemblyJarName in assembly := "migrator.jar",
    riffRaffPackageType := assembly.value,
    riffRaffArtifactFile := "migrator.zip",
    riffRaffArtifactPublishPath := "migrator",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk" % "1.9.23",
      "org.apache.commons" % "commons-lang3" % "3.3.2",
      "org.scalikejdbc" %% "scalikejdbc"       % "2.2.5",
      "com.oracle" % "jdbc_11g" % "11.2.0.3.0",
      "ch.qos.logback"  %  "logback-classic"   % "1.1.2"
    )
  )