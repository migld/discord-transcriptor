package com.discordtranscriptor.services

import com.discordtranscriptor.models._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.akkahttp.AkkaHttpBackend

import scala.concurrent.{ExecutionContext, Future}

case class ClaudeMessage(role: String, content: String)
case class ClaudeRequest(
  model:      String,
  max_tokens: Int,
  system:     String,
  messages:   List[ClaudeMessage]
)
case class ClaudeContentBlock(`type`: String, text: String)
case class ClaudeResponse(content: List[ClaudeContentBlock])

class NpcEngine(implicit ec: ExecutionContext) extends LazyLogging {

  private val config   = ConfigFactory.load()
  private val apiKey   = config.getString("claude.api-key")
  private val model    = config.getString("claude.model")

  implicit private val backend = AkkaHttpBackend()

  // Ask an NPC a question and get a response
  def talk(
    npc:      Npc,
    role:     NpcRole,
    scenario: ScenarioData,
    history:  Seq[GameMessage],
    message:  String
  ): Future[String] = {

    val systemPrompt = buildSystemPrompt(npc, role, scenario)
    val messages     = buildMessageHistory(history, message)

    val request = ClaudeRequest(
      model      = model,
      max_tokens = 400,
      system     = systemPrompt,
      messages   = messages
    )

    if (apiKey == "mock") {
      val mockResponses = List(
        s"I have no idea what you're implying. I was nowhere near there that night.",
        s"Look, ${scenario.victimName} and I had our differences, sure — but doesn't everyone?",
        s"I already told you my alibi. Why do you keep pressing me on this?",
        s"That's... that's a strange thing to bring up. Where did you hear that?",
        s"I saw nothing. I heard nothing. I was minding my own business all evening.",
        s"You think I did this? That's — that's absurd. I never argued with anyone about this.",
        s"I remember seeing something odd that night, but I don't see what that has to do with me.",
        s"Always poking around, aren't you. I never said I liked everyone here, but murder? Come on.",
        s"I was fixing a drink, I saw nothing. Now please — leave me alone."
      )
      val idx = math.abs((npc.name + message).hashCode) % mockResponses.size
      return Future.successful(mockResponses(idx))
    }

    val httpRequest = basicRequest
      .post(uri"https://api.anthropic.com/v1/messages")
      .header("x-api-key", apiKey)
      .header("anthropic-version", "2023-06-01")
      .contentType("application/json")
      .body(request.asJson.noSpaces)
      .response(asJson[ClaudeResponse])

    backend.send(httpRequest).flatMap {
      _.body match {
        case Right(resp) =>
          val text = resp.content.headOption.map(_.text).getOrElse("...")
          Future.successful(text)
        case Left(err) =>
          logger.error(s"Claude API error: $err")
          Future.failed(new RuntimeException(s"Claude API failed: $err"))
      }
    }
  }

  // Scan NPC response for clue keywords and extract clues
  def extractClues(npcResponse: String, npc: Npc, scenario: ScenarioData): List[(String, String)] = {
    val cluePatterns = List(
      ("Suspicious timing",   "mention of specific time"),
      ("Physical evidence",   "mention of an object"),
      ("Relationship strain", "mention of an argument or tension"),
      ("Alibi inconsistency", "contradictory details about their whereabouts")
    )

    // Simple heuristic: if response contains certain keywords, surface a clue
    val keywords = Map(
      "argue"    -> ("Heated Argument", s"${npc.name} mentioned an argument near the time of the murder."),
      "saw"      -> ("Eyewitness Detail", s"${npc.name} claims to have seen something that night."),
      "remember" -> ("Recovered Memory", s"${npc.name} recalled a specific detail under questioning."),
      "never"    -> ("Strong Denial", s"${npc.name} was unusually emphatic in their denial."),
      "always"   -> ("Habit Pattern", s"${npc.name} revealed a routine that may be relevant.")
    )

    keywords.collect {
      case (keyword, (title, description)) if npcResponse.toLowerCase.contains(keyword) =>
        (title, description)
    }.toList.take(1) // max one clue per message
  }

  private def buildSystemPrompt(npc: Npc, role: NpcRole, scenario: ScenarioData): String = {
    val isKiller = role.motive.isDefined
    val patterns = npc.speechPatterns.mkString(", ")

    val guiltDirective = if (isKiller)
      s"""You ARE the killer. You committed the murder because: ${role.motive.get}.
         |You must NEVER admit this. Lie, deflect, and misdirect — but stay believable.
         |Occasionally give yourself away in subtle ways (a slip, an odd reaction, too much detail)."""
    else
      s"""You are INNOCENT. You did not kill ${scenario.victimName}.
         |You may know small things that seem suspicious but are ultimately harmless.
         |Be honest but cagey — you're nervous about being accused."""

    s"""You are ${npc.name}, a suspect in a murder mystery game.
       |
       |YOUR PERSONALITY:
       |${npc.personality}
       |
       |HOW YOU SPEAK:
       |${if (patterns.nonEmpty) patterns else "Naturally and conversationally."}
       |
       |THE SITUATION:
       |${scenario.victimName} has been found dead. You are one of the suspects.
       |Your alibi: You ${role.alibi}.
       |Your relationship to the victim: ${role.relationshipToVictim}.
       |
       |$guiltDirective
       |
       |RULES:
       |- Stay in character at all times. Never break the fourth wall.
       |- Keep responses to 2-4 sentences. You're nervous and guarded.
       |- React naturally to being accused or pressed hard.
       |- Reference your real personality and speech patterns.
       |- This is a text-based noir detective game — lean into the atmosphere."""
  }

  private def buildMessageHistory(history: Seq[GameMessage], newMessage: String): List[ClaudeMessage] = {
    val prior = history.map { msg =>
      ClaudeMessage(
        role    = if (msg.sender == "player") "user" else "assistant",
        content = msg.content
      )
    }.toList

    prior :+ ClaudeMessage("user", newMessage)
  }
}
