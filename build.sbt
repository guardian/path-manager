addCommandAlias("dist", ";riffRaffArtifact")

import play.sbt.PlayImport.PlayKeys._
import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd

name := "path-manager"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

version := "1.0"

lazy val dependencies = Seq(
  "com.amazonaws" % "aws-java-sdk" % "1.11.86",
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.5.1",
  "com.gu" % "kinesis-logback-appender" % "1.3.0",
  "org.scalatestplus" %% "play" % "1.4.0" % "test"
)

lazy val pathManager = project.in(file("path-manager"))
  .enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging)
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(
    javaOptions in Universal ++= Seq(
      "-Dpidfile.path=/dev/null",
      "-J-XX:MaxRAMFraction=2",
      "-J-XX:InitialRAMFraction=2",
      "-J-XX:MaxMetaspaceSize=500m",
      "-J-XX:+UseConcMarkSweepGC",
      "-J-XX:+PrintGCDetails",
      "-J-XX:+PrintGCDateStamps",
      s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
    ),
    serverLoading in Debian := Systemd,
    debianPackageDependencies := Seq("openjdk-8-jre-headless"),
    maintainer := "Editorial Tools Developers <digitalcms.dev@theguardian.com>",
    packageSummary := description.value,
    packageDescription := description.value,
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
    riffRaffPackageType := (packageBin in Debian).value,
    riffRaffPackageName := name.value,
    riffRaffManifestProjectName := s"editorial-tools:${name.value}",
    riffRaffBuildIdentifier :=  Option(System.getenv("BUILD_NUMBER")).getOrElse("DEV"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestBranch := Option(System.getenv("BRANCH_NAME")).getOrElse("unknown_branch")
  )
