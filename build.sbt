name := "innodata"

version := "0.1"

scalaVersion := "2.12.6"

lazy val akkaHttpV = "10.1.5"
lazy val akkaV = "2.5.16"
lazy val circeV = "0.9.3"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "com.typesafe.akka" %% "akka-slf4j" % akkaV,
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",
  "io.circe" %% "circe-core" % circeV,
  "io.circe" %% "circe-generic" % circeV,
  "io.circe" %% "circe-parser" % circeV,
  "com.iheart" %% "ficus" % "1.4.3",

  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaV % Test
)

Test / parallelExecution := false