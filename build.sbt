
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq( 
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.typesafe.play" %% "play-ws" % "2.3.8",
  "com.typesafe.play" %% "play-json" % "2.3.4",
  "joda-time"  % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7"
)


