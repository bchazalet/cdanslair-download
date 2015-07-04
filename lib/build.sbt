
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

name := "cdanslair-lib"

lazy val root = project.in(file(".")).
  aggregate(cdanslairlibJS, cdanslairlibJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val cdanslairlib = crossProject.in(file(".")).
  settings(
    name := "cdanslairlib",
    version := "2.0-SNAPSHOT",
    scalaVersion := "2.11.6",
    libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.2.8"
  ).
  jvmSettings(
    // Add JVM-specific settings here
    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
      "joda-time"  % "joda-time" % "2.7",
      "org.joda" % "joda-convert" % "1.7",
      "com.github.scopt" %% "scopt" % "3.3.0",
      "com.ning" % "async-http-client" % "1.9.22"
    )
  ).
  jsSettings(
    // Add JS-specific settings here
  )

lazy val cdanslairlibJVM = cdanslairlib.jvm.enablePlugins(JavaAppPackaging)
lazy val cdanslairlibJS = cdanslairlib.js
