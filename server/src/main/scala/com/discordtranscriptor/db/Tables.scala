package com.discordtranscriptor.db

import com.discordtranscriptor.models._
import slick.jdbc.PostgresProfile.api._
import java.time.OffsetDateTime
import java.util.UUID

object Tables {

  // --- Users ---
  class UsersTable(tag: Tag) extends Table[User](tag, "users") {
    def id        = column[String]("id", O.PrimaryKey)
    def username  = column[String]("username")
    def avatar    = column[Option[String]]("avatar")
    def createdAt = column[OffsetDateTime]("created_at")

    def * = (id, username, avatar, createdAt).mapTo[User]
  }
  val users = TableQuery[UsersTable]

  // --- NPCs ---
  class NpcsTable(tag: Tag) extends Table[Npc](tag, "npcs") {
    def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def discordUserId   = column[Option[String]]("discord_user_id")
    def name            = column[String]("name")
    def avatarUrl       = column[Option[String]]("avatar_url")
    def personality     = column[String]("personality")
    def speechPatterns  = column[List[String]]("speech_patterns")
    def createdAt       = column[OffsetDateTime]("created_at")

    def * = (id, discordUserId, name, avatarUrl, personality, speechPatterns, createdAt).mapTo[Npc]
  }
  val npcs = TableQuery[NpcsTable]

  // --- Game Sessions ---
  class GameSessionsTable(tag: Tag) extends Table[GameSession](tag, "game_sessions") {
    def id            = column[UUID]("id", O.PrimaryKey)
    def playerId      = column[String]("player_id")
    def victimNpcId   = column[Int]("victim_npc_id")
    def killerNpcId   = column[Int]("killer_npc_id")
    def scenario      = column[String]("scenario")   // JSONB stored as String
    def status        = column[String]("status")
    def accusedNpcId  = column[Option[Int]]("accused_npc_id")
    def startedAt     = column[OffsetDateTime]("started_at")
    def endedAt       = column[Option[OffsetDateTime]]("ended_at")

    def * = (id, playerId, victimNpcId, killerNpcId, scenario, status, accusedNpcId, startedAt, endedAt).mapTo[GameSession]
  }
  val gameSessions = TableQuery[GameSessionsTable]

  // --- Game Messages ---
  class GameMessagesTable(tag: Tag) extends Table[GameMessage](tag, "game_messages") {
    def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def sessionId = column[UUID]("session_id")
    def sender    = column[String]("sender")
    def npcId     = column[Option[Int]]("npc_id")
    def content   = column[String]("content")
    def createdAt = column[OffsetDateTime]("created_at")

    def * = (id, sessionId, sender, npcId, content, createdAt).mapTo[GameMessage]
  }
  val gameMessages = TableQuery[GameMessagesTable]

  // --- Clues ---
  class CluesTable(tag: Tag) extends Table[Clue](tag, "clues") {
    def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def sessionId    = column[UUID]("session_id")
    def title        = column[String]("title")
    def description  = column[String]("description")
    def location     = column[Option[String]]("location")
    def discoveredAt = column[OffsetDateTime]("discovered_at")

    def * = (id, sessionId, title, description, location, discoveredAt).mapTo[Clue]
  }
  val clues = TableQuery[CluesTable]
}
