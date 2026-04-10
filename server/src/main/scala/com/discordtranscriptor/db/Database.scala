package com.discordtranscriptor.db

import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api._

object Database {
  private val config = ConfigFactory.load()

  val db: slick.jdbc.JdbcBackend.Database =
    slick.jdbc.JdbcBackend.Database.forURL(
      url    = config.getString("database.url"),
      driver = config.getString("database.driver")
    )
}
