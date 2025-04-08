ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.2"

lazy val root = (project in file("."))
  .settings(
    name := "stream-processing",
    idePackagePrefix := Some("org.tcc2.streaming")
  )

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.6"
libraryDependencies += "org.apache.kafka" % "kafka-streams" % "4.0.0"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17"