ThisBuild / scalaVersion := "2.13.1"

ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature")

ThisBuild / parallelExecution in Test := false

val akkaVersion = "2.6.4"
val akkaHttpVersion = "10.1.11"

// Dependencies
val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
val postgreSQLDriver = "org.postgresql" % "postgresql" % "42.2.12"
val hikariCP = "com.zaxxer" % "HikariCP" % "3.4.2"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % "test"
val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.4" % Test

lazy val root = (project in file("."))
  .aggregate(core, pb, cdc)

lazy val core = {
  (project in file("core"))
    .settings {
      name := "beard"
      version := "0.1"
      libraryDependencies := Seq(
        logback,
        "io.spray" %% "spray-json" % "1.3.5",
        "com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.4",
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        akkaStream,
        "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.2",
        "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
        "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        postgreSQLDriver,
        hikariCP,
        "org.scalactic" %% "scalactic" % "3.1.1",
        scalaTest,
        akkaTestKit
      )
    }
    .dependsOn(pb, cdc)
}

lazy val pb = (project in file("pb")).settings(
  PB.targets in Compile := Seq(
    scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
  )
)

lazy val cdc = (project in file("cdc"))
  .settings {
    name := "cdc"
    version := "0.1"
    libraryDependencies := Seq(
      "com.lihaoyi" %% "fastparse" % "2.2.2",
      akkaStream,
      postgreSQLDriver,
      hikariCP,
      scalaTest,
      akkaTestKit,
      akkaStreamTestKit,
      "jakarta.xml.bind" % "jakarta.xml.bind-api" % "2.3.2" % Test
    )
  }