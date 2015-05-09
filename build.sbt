name := "sntlm"

version := "0.0.1"

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-unchecked", "-deprecation" , "-feature")

mainClass in assembly := Some("sntlm.Main")

jarName in assembly := "sntlm.jar"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.4.1",
  "com.github.scopt"  %% "scopt"          % "3.3.0",
  "com.typesafe.akka" %% "akka-actor"     % "2.3.10",
  "ch.qos.logback"    % "logback-classic" % "1.1.3"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.+" % "test"

initialCommands in console := """
    |import sntlm._
    |""".stripMargin

