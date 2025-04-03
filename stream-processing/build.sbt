ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.2"

lazy val root = (project in file("."))
  .settings(
    name := "stream-processing",
    idePackagePrefix := Some("org.tcc2.streaming")
  )

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.6"
