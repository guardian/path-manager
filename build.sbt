addCommandAlias("dist", ";riffRaffArtifact")

import play.PlayImport.PlayKeys._

name := "path-manager"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

version := "1.0"

lazy val dependencies = Seq(
  "com.amazonaws" % "aws-java-sdk" % "1.9.23",
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.2",
  "org.scalatestplus" %% "play" % "1.1.0" % "test"
)

lazy val pathManager = project.in(file("path-manager"))
  .enablePlugins(PlayScala, RiffRaffArtifact, UniversalPlugin)
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(
    scalaVersion := "2.11.6",
    scalaVersion in ThisBuild := "2.11.6",
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
    doc in Compile <<= target.map(_ / "none"),
    fork in Test := false,
    name := "path-manager",
    playDefaultPort := 10000,
    libraryDependencies ++= dependencies,
    packageName in Universal := normalizedName.value,
    topLevelDirectory in Universal := Some(normalizedName.value),
    riffRaffPackageType := (packageZipTarball in config("universal")).value,
    riffRaffPackageName := name.value,
    riffRaffManifestProjectName := s"editorial-tools:${name.value}",
    riffRaffBuildIdentifier :=  Option(System.getenv("CIRCLE_BUILD_NUM")).getOrElse("dev"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestBranch := Option(System.getenv("CIRCLE_BRANCH")).getOrElse("dev")
  )
