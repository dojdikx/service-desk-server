package com.servicedesk.server.db

import com.servicedesk.server.config.AppConfig
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    private var database: Database? = null

    fun init() {
        val (url, user, password) = AppConfig.jdbcCredentials
        database = Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = user,
            password = password,
        )
    }

    fun close() {
        database = null
    }
}
