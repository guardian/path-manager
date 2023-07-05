// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.11")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "1.1.9")

addSbtPlugin("com.github.ehsanyou" % "sbt-docker-compose" % "2.0.0")

libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts Artifact("jdeb", "jar", "jar")

/*
   Because scala-xml has not be updated to 2.x in sbt yet but has in sbt-native-packager
   See: https://github.com/scala/bug/issues/12632

   This effectively overrides the safeguards (early-semver) put in place by the library authors ensuring binary compatibility.
   We consider this a safe operation because it only affects the compilation of build.sbt, not of the application build itself
 */
libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)