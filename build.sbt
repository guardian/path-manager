import play.sbt.PlayImport.PlayKeys._

name := "path-manager"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

version := "1.0"

val awsVersion = "1.12.129"

lazy val dependencies = Seq(
  "com.github.jnr" % "jnr-unixsocket" % "0.38.22" % "test",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-ec2" % awsVersion,
  "org.apache.commons" % "commons-lang3" % "3.11",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.6",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test",
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % "test"
)

enablePlugins(DockerCompose)

addCommandAlias("start", "" +
  "; dockerCompose up -d" +
  "; compile" +
  "; pathManager/run")

lazy val pathManager = project.in(file("path-manager"))
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin)
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(
    Universal / javaOptions ++= Seq(
      "-Dpidfile.path=/dev/null",
      "-J-XX:MaxRAMFraction=2",
      "-J-XX:InitialRAMFraction=2",
      "-J-XX:MaxMetaspaceSize=500m",
      "-J-XX:+UseConcMarkSweepGC",
      "-J-XX:+PrintGCDetails",
      "-J-XX:+PrintGCDateStamps",
      s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
    ),
    debianPackageDependencies := Seq("java11-runtime-headless"),
    maintainer := "Editorial Tools Developers <digitalcms.dev@theguardian.com>",
    packageSummary := description.value,
    packageDescription := description.value,
    scalaVersion := "2.13.7",
    ThisBuild / scalaVersion := "2.13.7",
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
    Compile / doc := (target.value / "none"),
    Test / fork := false,
    name := "path-manager",
    playDefaultPort := 10000,
    libraryDependencies ++= dependencies,
    //Necessary to override jackson-databind versions due to AWS and Play incompatibility
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4",
    Universal / packageName := normalizedName.value,
    Universal/ topLevelDirectory := Some(normalizedName.value),
  )
