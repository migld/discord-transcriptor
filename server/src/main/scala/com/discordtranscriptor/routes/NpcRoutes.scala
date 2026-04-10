package com.discordtranscriptor.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.discordtranscriptor.db.Database.db
import com.discordtranscriptor.db.Tables._
import com.discordtranscriptor.models.Models._
import com.discordtranscriptor.services.AuthService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class NpcRoutes(authService: AuthService)(implicit ec: ExecutionContext) {

  val routes: Route = pathPrefix("npcs") {
    headerValueByName("Authorization") { header =>
      val token = header.stripPrefix("Bearer ")
      authService.validateJwt(token) match {
        case None => complete(StatusCodes.Unauthorized, "Invalid token")
        case Some(_) =>
          // GET /npcs — list all NPCs
          (get & pathEnd) {
            onComplete(db.run(npcs.result)) {
              case Success(all) => complete(all)
              case Failure(ex)  => complete(StatusCodes.InternalServerError, ex.getMessage)
            }
          }
      }
    }
  }
}
