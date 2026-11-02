addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.10")

addSbtPlugin(("com.github.ehsanyou" % "sbt-docker-compose" % "2.0.0")
  .exclude("com.fasterxml.jackson.datatype", "jackson-datatype-guava"))

libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts Artifact("jdeb", "jar", "jar")

addDependencyTreePlugin

// Align Jackson pulled in by the Docker Compose and web build plugins.
val jacksonVersion = "2.18.11"
dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core",
  "com.fasterxml.jackson.core" % "jackson-databind",
  "com.fasterxml.jackson.core" % "jackson-annotations",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava",
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-base",
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider",
  "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations"
).map(_ % jacksonVersion)

// Reintroduce the aligned module without upgrading Guava already supplied by Docker client.
// Exclusions on dependencyOverrides alone are not applied by the resolver.
libraryDependencies += ("com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion)
  .exclude("com.google.guava", "guava")
