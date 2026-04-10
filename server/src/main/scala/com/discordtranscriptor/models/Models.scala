package com.discordtranscriptor.models

import java.time.OffsetDateTime
import java.util.UUID
import io.circe.generic.semiauto._
import io.circe._

// --- Domain Models ---

case class User(
  id:        String,
  username:  String,
  avatar:    Option[String],
  createdAt: OffsetDateTime
)

case class Npc(
  id:             Int,
  discordUserId:  Option[String],
  name:           String,
  avatarUrl:      Option[String],
  personality:    String,
  speechPatterns: List[String],
  createdAt:      OffsetDateTime
)

case class GameSession(
  id:           UUID,
  playerId:     String,
  victimNpcId:  Int,
  killerNpcId:  Int,
  scenario:     String,           // JSON blob — see ScenarioData
  status:       String,           // active | accused | solved
  accusedNpcId: Option[Int],
  startedAt:    OffsetDateTime,
  endedAt:      Option[OffsetDateTime]
)

case class GameMessage(
  id:        Int,
  sessionId: UUID,
  sender:    String,              // player | npc
  npcId:     Option[Int],
  content:   String,
  createdAt: OffsetDateTime
)

case class Clue(
  id:           Int,
  sessionId:    UUID,
  title:        String,
  description:  String,
  location:     Option[String],
  discoveredAt: OffsetDateTime
)

// --- Scenario Data (stored as JSON inside game_sessions.scenario) ---

case class NpcRole(
  npcId:               Int,
  alibi:               String,
  relationshipToVictim: String,
  motive:              Option[String]   // only set for the killer
)

case class ScenarioData(
  title:        String,             // e.g. "The Manor at Midnight"
  openingScene: String,             // narrative intro shown to the player
  victimName:   String,
  causeOfDeath: String,
  location:     String,
  npcRoles:     List[NpcRole]
)

// --- API Request/Response Shapes ---

case class TalkRequest(npcId: Int, message: String)
case class TalkResponse(reply: String, newClues: List[Clue])
case class AccuseRequest(npcId: Int)
case class AccuseResponse(correct: Boolean, message: String)

// --- Circe codecs ---

object Models {
  implicit val npcRoleEncoder: Encoder[NpcRole]           = deriveEncoder
  implicit val npcRoleDecoder: Decoder[NpcRole]           = deriveDecoder
  implicit val scenarioEncoder: Encoder[ScenarioData]     = deriveEncoder
  implicit val scenarioDecoder: Decoder[ScenarioData]     = deriveDecoder
  implicit val userEncoder: Encoder[User]                 = deriveEncoder
  implicit val npcEncoder: Encoder[Npc]                   = deriveEncoder
  implicit val sessionEncoder: Encoder[GameSession]       = deriveEncoder
  implicit val messageEncoder: Encoder[GameMessage]       = deriveEncoder
  implicit val clueEncoder: Encoder[Clue]                 = deriveEncoder
  implicit val talkReqDecoder: Decoder[TalkRequest]       = deriveDecoder
  implicit val talkRespEncoder: Encoder[TalkResponse]     = deriveEncoder
  implicit val accuseReqDecoder: Decoder[AccuseRequest]   = deriveDecoder
  implicit val accuseRespEncoder: Encoder[AccuseResponse] = deriveEncoder
}
