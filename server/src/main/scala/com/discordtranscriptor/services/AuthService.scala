package com.discordtranscriptor.services

import com.discordtranscriptor.db.Database.db
import com.discordtranscriptor.db.Tables._
import com.discordtranscriptor.models.User
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.parser._
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import slick.jdbc.PostgresProfile.api._
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.akkahttp.AkkaHttpBackend

import java.time.{Clock, OffsetDateTime}
import scala.concurrent.{ExecutionContext, Future}

case class DiscordTokenResponse(
  access_token:  String,
  token_type:    String,
  expires_in:    Int,
  refresh_token: String,
  scope:         String
)

case class DiscordUser(
  id:       String,
  username: String,
  avatar:   Option[String]
)

class AuthService(implicit ec: ExecutionContext) extends LazyLogging {

  private val config        = ConfigFactory.load()
  private val jwtSecret     = config.getString("app.jwt-secret")
  private val jwtExpiryHrs  = config.getInt("app.jwt-expiry-hours")
  private val clientId      = config.getString("discord.client-id")
  private val clientSecret  = config.getString("discord.client-secret")
  private val redirectUri   = config.getString("discord.redirect-uri")

  implicit private val backend = AkkaHttpBackend()
  implicit private val clock   = Clock.systemUTC()

  // Exchange Discord OAuth code for user info, upsert user, return JWT
  def handleOAuthCallback(code: String): Future[String] = {
    for {
      tokenResp   <- exchangeCode(code)
      discordUser <- fetchDiscordUser(tokenResp.access_token)
      user        <- upsertUser(discordUser)
      token        = issueJwt(user)
    } yield token
  }

  private def exchangeCode(code: String): Future[DiscordTokenResponse] = {
    val request = basicRequest
      .post(uri"https://discord.com/api/oauth2/token")
      .body(Map(
        "client_id"     -> clientId,
        "client_secret" -> clientSecret,
        "grant_type"    -> "authorization_code",
        "code"          -> code,
        "redirect_uri"  -> redirectUri
      ))
      .response(asJson[DiscordTokenResponse])

    backend.send(request).flatMap {
      _.body match {
        case Right(t) => Future.successful(t)
        case Left(e)  => Future.failed(new RuntimeException(s"Discord token exchange failed: $e"))
      }
    }
  }

  private def fetchDiscordUser(accessToken: String): Future[DiscordUser] = {
    val request = basicRequest
      .get(uri"https://discord.com/api/users/@me")
      .header("Authorization", s"Bearer $accessToken")
      .response(asJson[DiscordUser])

    backend.send(request).flatMap {
      _.body match {
        case Right(u) => Future.successful(u)
        case Left(e)  => Future.failed(new RuntimeException(s"Discord user fetch failed: $e"))
      }
    }
  }

  private def upsertUser(discordUser: DiscordUser): Future[User] = {
    val user = User(
      id        = discordUser.id,
      username  = discordUser.username,
      avatar    = discordUser.avatar,
      createdAt = OffsetDateTime.now()
    )

    val upsert = users.insertOrUpdate(user)
    db.run(upsert).map(_ => user)
  }

  // Dev-only: skip Discord OAuth, create/upsert a test user and return a JWT
  def devLogin(): Future[String] = {
    val testUser = User(
      id        = "dev-user-001",
      username  = "DevPlayer",
      avatar    = None,
      createdAt = OffsetDateTime.now()
    )
    db.run(users.insertOrUpdate(testUser)).map(_ => issueJwt(testUser))
  }

  def issueJwt(user: User): String = {
    val claim = JwtClaim(
      content    = s"""{"userId":"${user.id}","username":"${user.username}"}""",
      expiration = Some(java.time.Instant.now.plusSeconds(jwtExpiryHrs * 3600L).getEpochSecond)
    )
    JwtCirce.encode(claim, jwtSecret, JwtAlgorithm.HS256)
  }

  def validateJwt(token: String): Option[String] = {
    JwtCirce.decode(token, jwtSecret, Seq(JwtAlgorithm.HS256)).toOption
      .flatMap(claim => decode[Map[String, String]](claim.content).toOption)
      .flatMap(_.get("userId"))
  }
}
