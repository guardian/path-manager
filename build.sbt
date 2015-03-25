import com.typesafe.sbt.packager.Keys._
import play.PlayImport.PlayKeys._

name := "path-manager"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

version := "1.0"

libraryDependencies ++= Seq(
  ws,
  "com.amazonaws" % "aws-java-sdk" % "1.9.23",
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "org.scalatestplus" %% "play" % "1.1.0" % "test"
)

lazy val mainProject = project.in(file("."))
  .enablePlugins(PlayScala, RiffRaffArtifact)
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(
    playDefaultPort := 10000,
    // Never interested in the version number in the artifact name
    name in Universal := normalizedName.value,
    riffRaffPackageType := (packageZipTarball in config("universal")).value,
    scalaVersion := "2.11.4",
    scalaVersion in ThisBuild := "2.11.4",
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
    doc in Compile <<= target.map(_ / "none"),
    fork in Test := false
  )