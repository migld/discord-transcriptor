ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.discordtranscriptor"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "discord-transcriptor-server",

    libraryDependencies ++= Seq(
      // Akka HTTP
      "com.typesafe.akka" %% "akka-http"            % "10.5.3",
      "com.typesafe.akka" %% "akka-stream"          % "2.8.5",
      "com.typesafe.akka" %% "akka-actor-typed"     % "2.8.5",

      // JSON
      "io.circe"          %% "circe-core"            % "0.14.6",
      "io.circe"          %% "circe-generic"         % "0.14.6",
      "io.circe"          %% "circe-parser"          % "0.14.6",
      "de.heikoseeberger" %% "akka-http-circe"       % "1.39.2",

      // Database
      "com.typesafe.slick" %% "slick"                % "3.4.1",
      "com.typesafe.slick" %% "slick-hikaricp"       % "3.4.1",
      "org.postgresql"      % "postgresql"            % "42.7.1",

      // HTTP Client (Claude API + Discord OAuth)
      "com.softwaremill.sttp.client3" %% "core"      % "3.9.2",
      "com.softwaremill.sttp.client3" %% "circe"     % "3.9.2",
      "com.softwaremill.sttp.client3" %% "akka-http-backend" % "3.9.2",

      // JWT
      "com.github.jwt-scala" %% "jwt-circe"          % "10.0.1",

      // Config
      "com.typesafe"         % "config"               % "1.4.3",

      // Logging
      "ch.qos.logback"       % "logback-classic"      % "1.4.14",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

      // Testing
      "org.scalatest"       %% "scalatest"             % "3.2.17" % Test
    )
  )
