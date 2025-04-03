error id: `<none>`.
file://<WORKSPACE>/stream-processing/build.sbt
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 302
uri: file://<WORKSPACE>/stream-processing/build.sbt
text:
```scala
val scala3Version = "3.6.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "stream-processing",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-streams" % "3.4.0",
      "org.apache.kafka" %% "@@kafka-streams-scala" % "3.4.0",
      "com.typesafe.play" %% "play-json" % "2.9.4",
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "org.scalameta" %% "munit" % "1.0.0" % Test
    )
  )

```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.