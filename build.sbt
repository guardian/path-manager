import play.sbt.PlayImport.PlayKeys._
import scala.sys.process._

name := "path-manager"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

version := "1.0"

val awsVersion = "1.12.797"

val jacksonVersion  = "2.20.1"
val jacksons        = Seq(
  "com.fasterxml.jackson.core" % "jackson-core",
  "com.fasterxml.jackson.core" % "jackson-databind",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
).map(_ % jacksonVersion)

// Overrides additional jackson deps pulled in by pekko-serialization-jackson
// Play uses a newer version of Jackson than Pekko
val pekkoSerializationJacksonOverrides = Seq(
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
  "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
  "com.fasterxml.jackson.module"    %% "jackson-module-scala",
).map(_ % jacksonVersion)

lazy val dependencies = Seq(
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-ec2" % awsVersion,
  "org.apache.commons" % "commons-lang3" % "3.20.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "9.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % "test",
)

val projectDependencyOverrides = jacksons ++ pekkoSerializationJacksonOverrides

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
      "-J-XX:InitialRAMPercentage=50",
      "-J-XX:MaxRAMPercentage=50",
      "-J-XX:MaxMetaspaceSize=500m",
      "-J-XX:+PrintGCDetails",
      s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
    ),
    debianPackageDependencies := Seq("java21-runtime-headless"),
    maintainer := "Editorial Tools Developers <digitalcms.dev@theguardian.com>",
    packageSummary := description.value,
    packageDescription := description.value,
    scalaVersion := "2.13.18",
    ThisBuild / scalaVersion := "2.13.18",
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
    Compile / doc := (target.value / "none"),
    Test / fork := false,
    name := "path-manager",
    playDefaultPort := 10000,
    libraryDependencies ++= dependencies,
    dependencyOverrides ++= projectDependencyOverrides,
    Universal / packageName := normalizedName.value,
    Universal/ topLevelDirectory := Some(normalizedName.value),
    Test / testOptions += Tests.Setup(_ => {
      println(s"Starting Docker containers for tests")
      "docker compose up -d".!
    }),
    Test / testOptions += Tests.Cleanup(_ => {
      println(s"Stopping Docker containers for tests")
      "docker compose rm --stop --force".!
    }),
  )
