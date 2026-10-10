package com.servicedesk.server.plugins

import com.servicedesk.server.config.AppConfig
import io.ktor.server.application.Application
import org.flywaydb.core.Flyway

fun Application.configureFlyway() {
    val migrateEnabled = System.getenv("FLYWAY_MIGRATE")?.toBooleanStrictOrNull() ?: true
    if (!migrateEnabled) return

    val (jdbcUrl, user, password) = AppConfig.jdbcCredentials

    Flyway.configure()
        .dataSource(jdbcUrl, user, password)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()
        .migrate()
}
