
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

enablePlugins(JavaAppPackaging)

version := "1.2"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-nop" % "1.7.12",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.typesafe.play" %% "play-json" % "2.3.4",
  "joda-time"  % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "com.ning" % "async-http-client" % "1.9.22"
)
