package com.discordtranscriptor.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import com.discordtranscriptor.db.Database.db
import com.discordtranscriptor.db.Tables._
import com.discordtranscriptor.models._
import com.discordtranscriptor.models.Models._
import com.discordtranscriptor.services.{AuthService, GameEngine, NpcEngine}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class GameRoutes(
  authService: AuthService,
  gameEngine:  GameEngine,
  npcEngine:   NpcEngine
)(implicit ec: ExecutionContext) {

  // Directive: extract and validate JWT, return userId
  private def authenticated: Directive1[String] =
    headerValueByName("Authorization").flatMap { header =>
      val token = header.stripPrefix("Bearer ")
      authService.validateJwt(token) match {
        case Some(userId) => provide(userId)
        case None         => complete(StatusCodes.Unauthorized, "Invalid token")
      }
    }

  val routes: Route = pathPrefix("game") {
    authenticated { playerId =>
      concat(

        // POST /game/new — start a new game
        (path("new") & post) {
          onComplete(gameEngine.startGame(playerId)) {
            case Success(session) => complete(session)
            case Failure(ex)      => complete(StatusCodes.InternalServerError, ex.getMessage)
          }
        },

        // GET /game/:id — get session state + messages + clues
        (path(JavaUUID) & get) { sessionId =>
          val result = for {
            maybeSession <- gameEngine.getSession(sessionId)
            session       = maybeSession.getOrElse(throw new RuntimeException("Not found"))
            messages     <- gameEngine.getMessages(sessionId)
            clues        <- gameEngine.getClues(sessionId)
            allNpcs      <- db.run(npcs.result)
          } yield (session, messages, clues, allNpcs)

          onComplete(result) {
            case Success((session, messages, clues, allNpcs)) =>
              val scenario   = gameEngine.parseScenario(session)
              val suspectIds = scenario.npcRoles.map(_.npcId).toSet
              val suspects   = allNpcs.filter(n => suspectIds.contains(n.id))
              complete(Map(
                "session"  -> session.asJson,
                "suspects" -> suspects.asJson,
                "messages" -> messages.asJson,
                "clues"    -> clues.asJson,
                "scenario" -> Map(
                  "title"        -> scenario.title,
                  "openingScene" -> scenario.openingScene,
                  "victimName"   -> scenario.victimName,
                  "location"     -> scenario.location
                ).asJson
              ))
            case Failure(ex) =>
              complete(StatusCodes.NotFound, ex.getMessage)
          }
        },

        // POST /game/:id/talk — send message to an NPC
        (path(JavaUUID / "talk") & post) { sessionId =>
          entity(as[TalkRequest]) { req =>
            val result = for {
              maybeSession <- gameEngine.getSession(sessionId)
              session       = maybeSession.getOrElse(throw new RuntimeException("Session not found"))
              _             = if (session.status != "active") throw new RuntimeException("Game is over")
              scenario      = gameEngine.parseScenario(session)
              allNpcs      <- db.run(npcs.result)
              npc           = allNpcs.find(_.id == req.npcId).getOrElse(throw new RuntimeException("NPC not found"))
              role          = scenario.npcRoles.find(_.npcId == req.npcId).getOrElse(throw new RuntimeException("NPC not in this game"))
              history      <- gameEngine.getMessages(sessionId)
              _            <- gameEngine.saveMessage(sessionId, "player", Some(req.npcId), req.message)
              reply        <- npcEngine.talk(npc, role, scenario, history, req.message)
              _            <- gameEngine.saveMessage(sessionId, "npc", Some(req.npcId), reply)
              // Extract and save any clues surfaced by the NPC response
              rawClues      = npcEngine.extractClues(reply, npc, scenario)
              newClues     <- scala.concurrent.Future.sequence(
                                rawClues.map { case (title, desc) =>
                                  gameEngine.saveClue(sessionId, title, desc, Some(scenario.location))
                                }
                              )
            } yield TalkResponse(reply = reply, newClues = newClues)

            onComplete(result) {
              case Success(resp) => complete(resp)
              case Failure(ex)   => complete(StatusCodes.InternalServerError, ex.getMessage)
            }
          }
        },

        // POST /game/:id/accuse — make the final accusation
        (path(JavaUUID / "accuse") & post) { sessionId =>
          entity(as[AccuseRequest]) { req =>
            onComplete(gameEngine.accuse(sessionId, req.npcId)) {
              case Success((correct, _)) =>
                val msg = if (correct) "You got them. Justice is served."
                          else "Wrong. The real killer is still out there..."
                complete(AccuseResponse(correct = correct, message = msg))
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, ex.getMessage)
            }
          }
        },

        // GET /game/:id/reveal — full solution (only after accusation)
        (path(JavaUUID / "reveal") & get) { sessionId =>
          val result = for {
            maybeSession <- gameEngine.getSession(sessionId)
            session       = maybeSession.getOrElse(throw new RuntimeException("Not found"))
            _             = if (session.status == "active") throw new RuntimeException("Make an accusation first")
            allNpcs      <- db.run(npcs.result)
            killer        = allNpcs.find(_.id == session.killerNpcId)
            victim        = allNpcs.find(_.id == session.victimNpcId)
            scenario      = gameEngine.parseScenario(session)
            killerRole    = scenario.npcRoles.find(_.npcId == session.killerNpcId)
          } yield Map(
            "killerName"  -> killer.map(_.name).getOrElse("Unknown"),
            "victimName"  -> victim.map(_.name).getOrElse("Unknown"),
            "motive"      -> killerRole.flatMap(_.motive).getOrElse("Unknown"),
            "openingScene"-> scenario.openingScene,
            "playerWon"   -> (session.accusedNpcId.contains(session.killerNpcId)).toString
          )

          onComplete(result) {
            case Success(reveal) => complete(reveal)
            case Failure(ex)     => complete(StatusCodes.BadRequest, ex.getMessage)
          }
        }
      )
    }
  }
}
