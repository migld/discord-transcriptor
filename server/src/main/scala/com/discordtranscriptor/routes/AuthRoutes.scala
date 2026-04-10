package com.discordtranscriptor.routes

import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.model.headers.{HttpCookie, Location, RawHeader}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.discordtranscriptor.services.AuthService
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class AuthRoutes(authService: AuthService)(implicit ec: ExecutionContext) extends LazyLogging {

  private val config      = ConfigFactory.load()
  private val clientId    = config.getString("discord.client-id")
  private val redirectUri = config.getString("discord.redirect-uri")

  val routes: Route = pathPrefix("auth") {
    concat(
      // GET /auth/dev-login — skip Discord OAuth for local testing (Phase 1 only)
      path("dev-login") {
        get {
          onComplete(authService.devLogin()) {
            case Success(jwt) =>
              val frontendUrl = s"http://localhost:3000/auth/callback#token=$jwt"
              redirect(frontendUrl, StatusCodes.Found)
            case Failure(ex) =>
              logger.error("Dev login failed", ex)
              complete(StatusCodes.InternalServerError, "Dev login failed")
          }
        }
      },

      // GET /auth/login — redirect to Discord OAuth
      path("login") {
        get {
          val discordOAuthUrl =
            s"https://discord.com/api/oauth2/authorize" +
            s"?client_id=$clientId" +
            s"&redirect_uri=${java.net.URLEncoder.encode(redirectUri, "UTF-8")}" +
            s"&response_type=code" +
            s"&scope=identify"

          redirect(discordOAuthUrl, StatusCodes.Found)
        }
      },

      // GET /auth/callback?code=... — Discord sends user back here
      path("callback") {
        get {
          parameter("code") { code =>
            onComplete(authService.handleOAuthCallback(code)) {
              case Success(jwt) =>
                // Redirect to frontend with token in URL hash (React picks it up)
                val frontendUrl = s"http://localhost:3000/auth/callback#token=$jwt"
                redirect(frontendUrl, StatusCodes.Found)
              case Failure(ex) =>
                logger.error("OAuth callback failed", ex)
                complete(StatusCodes.InternalServerError, "Authentication failed")
            }
          }
        }
      },

      // GET /auth/me — return current user info (requires JWT)
      path("me") {
        get {
          headerValueByName("Authorization") { authHeader =>
            val token = authHeader.stripPrefix("Bearer ")
            authService.validateJwt(token) match {
              case Some(userId) =>
                complete(Map("userId" -> userId))
              case None =>
                complete(StatusCodes.Unauthorized, "Invalid token")
            }
          }
        }
      }
    )
  }
}
