package com.discordtranscriptor.services

import com.discordtranscriptor.db.Database.db
import com.discordtranscriptor.db.Tables._
import com.discordtranscriptor.models._
import com.discordtranscriptor.models.Models._
import io.circe.syntax._
import io.circe.parser._
import slick.jdbc.PostgresProfile.api._

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class GameEngine(implicit ec: ExecutionContext) {

  private val locations = List(
    "The Study", "The Kitchen", "The Garden", "The Library", "The Cellar"
  )

  private val causesOfDeath = List(
    "poisoned wine", "a blunt object", "a mysterious letter opener",
    "something in their coffee", "an apparent fall down the stairs"
  )

  private val motives = List(
    "a bitter dispute over money",
    "unrequited love turned obsession",
    "a secret threatening to surface",
    "jealousy over a recent promotion",
    "revenge for an old betrayal"
  )

  private val alibis = List(
    "claims to have been reading alone in the library",
    "says they were fixing a drink in the kitchen",
    "insists they were outside in the garden all evening",
    "swears they were asleep in their room",
    "says they were on a phone call with someone from out of town"
  )

  // Start a new game: pick victim, killer, assign roles, generate scenario
  def startGame(playerId: String): Future[GameSession] = {
    for {
      allNpcs  <- db.run(npcs.result)
      session  <- createSession(playerId, allNpcs)
    } yield session
  }

  private def createSession(playerId: String, allNpcs: Seq[Npc]): Future[GameSession] = {
    if (allNpcs.size < 2)
      return Future.failed(new RuntimeException("Need at least 2 NPCs to start a game"))

    val shuffled  = Random.shuffle(allNpcs.toList)
    val victim    = shuffled.head
    val remaining = shuffled.tail
    val killer    = remaining.head
    val suspects  = remaining  // everyone except the victim is a suspect

    val shuffledAlibi = Random.shuffle(alibis)
    val killerMotive  = Random.shuffle(motives).head

    val npcRoles = suspects.zipWithIndex.map { case (npc, i) =>
      NpcRole(
        npcId                = npc.id,
        alibi                = shuffledAlibi(i % shuffledAlibi.size),
        relationshipToVictim = generateRelationship(npc, victim),
        motive               = if (npc.id == killer.id) Some(killerMotive) else None
      )
    }

    val scenario = ScenarioData(
      title        = s"The ${Random.shuffle(List("Manor", "Estate", "Grange", "Hall", "Lodge")).head} at Midnight",
      openingScene = buildOpeningScene(victim, locations(Random.nextInt(locations.size)), causesOfDeath(Random.nextInt(causesOfDeath.size))),
      victimName   = victim.name,
      causeOfDeath = causesOfDeath(Random.nextInt(causesOfDeath.size)),
      location     = locations(Random.nextInt(locations.size)),
      npcRoles     = npcRoles
    )

    val session = GameSession(
      id           = UUID.randomUUID(),
      playerId     = playerId,
      victimNpcId  = victim.id,
      killerNpcId  = killer.id,
      scenario     = scenario.asJson.noSpaces,
      status       = "active",
      accusedNpcId = None,
      startedAt    = OffsetDateTime.now(),
      endedAt      = None
    )

    db.run(gameSessions += session).map(_ => session)
  }

  def getSession(sessionId: UUID): Future[Option[GameSession]] =
    db.run(gameSessions.filter(_.id === sessionId).result.headOption)

  def getMessages(sessionId: UUID): Future[Seq[GameMessage]] =
    db.run(gameMessages.filter(_.sessionId === sessionId).sortBy(_.createdAt).result)

  def getClues(sessionId: UUID): Future[Seq[Clue]] =
    db.run(clues.filter(_.sessionId === sessionId).sortBy(_.discoveredAt).result)

  def saveMessage(sessionId: UUID, sender: String, npcId: Option[Int], content: String): Future[Unit] = {
    val msg = GameMessage(0, sessionId, sender, npcId, content, OffsetDateTime.now())
    db.run(gameMessages += msg).map(_ => ())
  }

  def saveClue(sessionId: UUID, title: String, description: String, location: Option[String]): Future[Clue] = {
    val clue = Clue(0, sessionId, title, description, location, OffsetDateTime.now())
    db.run((clues returning clues) += clue)
  }

  def accuse(sessionId: UUID, accusedNpcId: Int): Future[(Boolean, GameSession)] = {
    for {
      maybeSession <- getSession(sessionId)
      session      = maybeSession.getOrElse(throw new RuntimeException("Session not found"))
      correct      = session.killerNpcId == accusedNpcId
      status       = if (correct) "solved" else "accused"
      updated      = session.copy(status = status, accusedNpcId = Some(accusedNpcId), endedAt = Some(OffsetDateTime.now()))
      _           <- db.run(gameSessions.filter(_.id === sessionId).update(updated))
    } yield (correct, updated)
  }

  def parseScenario(session: GameSession): ScenarioData =
    decode[ScenarioData](session.scenario).getOrElse(throw new RuntimeException("Invalid scenario JSON"))

  private def buildOpeningScene(victim: Npc, location: String, cause: String): String =
    s"""The grandfather clock strikes midnight. The guests have gathered for what was meant to be
       |a quiet evening — but ${victim.name} has been found dead in ${location.toLowerCase},
       |the apparent victim of ${cause}. The doors are locked. No one has left.
       |One of the people in this house is a killer. It's up to you to find out who.""".stripMargin

  private def generateRelationship(npc: Npc, victim: Npc): String = {
    val relationships = List(
      s"old friends who had a recent falling out",
      s"colleagues with a history of rivalry",
      s"distant acquaintances who met through mutual friends",
      s"former roommates with unresolved tension",
      s"close confidants who shared many secrets"
    )
    Random.shuffle(relationships).head
  }
}
