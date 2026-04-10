package com.discordtranscriptor

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.discordtranscriptor.routes.{AuthRoutes, GameRoutes, NpcRoutes}
import com.discordtranscriptor.services.{AuthService, GameEngine, NpcEngine}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Server extends LazyLogging {

  def start(): Unit = {
    implicit val system: ActorSystem[Nothing] =
      ActorSystem(Behaviors.empty, "discord-transcriptor")
    implicit val ec: ExecutionContext = system.executionContext

    val config  = ConfigFactory.load()
    val port    = config.getInt("app.port")

    val authService = new AuthService()
    val gameEngine  = new GameEngine()
    val npcEngine   = new NpcEngine()

    val authRoutes  = new AuthRoutes(authService)
    val gameRoutes  = new GameRoutes(authService, gameEngine, npcEngine)
    val npcRoutes   = new NpcRoutes(authService)

    val corsHeaders = List(
      `Access-Control-Allow-Origin`.*,
      `Access-Control-Allow-Methods`(GET, POST, PUT, DELETE, OPTIONS),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type")
    )

    val routes: Route = respondWithHeaders(corsHeaders) {
      concat(
        options { complete("") },   // CORS preflight
        authRoutes.routes,
        gameRoutes.routes,
        npcRoutes.routes
      )
    }

    Http().newServerAt("0.0.0.0", port).bind(routes).onComplete {
      case Success(binding) =>
        logger.info(s"Server started at http://0.0.0.0:$port")
      case Failure(ex) =>
        logger.error(s"Failed to start server", ex)
        system.terminate()
    }
  }
}
