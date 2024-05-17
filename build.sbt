import play.sbt.PlayImport.PlayKeys._

name := "path-manager"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

version := "1.0"

val awsVersion = "1.12.583"

lazy val dependencies = Seq(
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-ec2" % awsVersion,
  "org.apache.commons" % "commons-lang3" % "3.14.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % "test",
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % "test",

  // By default, logstash-logback-encoder will tell jackson to dynamically discover all available jackson modules
  // on the classpath by calling objectMapper.findAndRegisterModules()
  // If jackson-module-jaxb-annotations is on the classpath, jackson will try to register it and fail because
  // javax.xml.bind:jaxb-api is missing. This was not an issue before Java 9, because javax.xml.bind:jaxb-api was
  // included in the JDK
  //
  // The options to solve this were either to add javaxb-api as a dependency, or to exclude jackson-module-jaxb-annotations.
  // We chose the latter because it is a smaller change and does not introduce a new dependency.
  //
  // See https://github.com/logfellow/logstash-logback-encoder/issues/1005

  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % "test" exclude("com.fasterxml.jackson.module", "jackson-module-jaxb-annotations"),

  // docker-testkit-impl-spotif depends on jnr-unixsocket:0.18, which doesn't support M1 silicon
  // see https://github.com/spotify/docker-client/pull/1221 (unmerged at present)
  "com.github.jnr" % "jnr-unixsocket" % "0.38.22" % "test" exclude("com.fasterxml.jackson.module", "jackson-module-jaxb-annotations")
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
    Universal / packageName := normalizedName.value,
    Universal/ topLevelDirectory := Some(normalizedName.value),
  )
