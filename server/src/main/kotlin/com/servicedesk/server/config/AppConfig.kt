package com.servicedesk.server.config

object AppConfig {
    val host: String = System.getenv("HOST") ?: "0.0.0.0"
    val port: Int = (System.getenv("PORT") ?: "8080").toInt()

    val databaseUrl: String by lazy {
        System.getenv("DATABASE_URL")?.substringBefore('?')
            ?: run {
                val dbHost = System.getenv("DB_HOST") ?: "localhost"
                val dbPort = System.getenv("DB_PORT") ?: "5433"
                val dbName = System.getenv("DB_NAME") ?: "servicedesk"
                "jdbc:postgresql://$dbHost:$dbPort/$dbName"
            }
    }

    val databaseUser: String =
        System.getenv("DATABASE_USER") ?: System.getenv("DB_USER") ?: "servicedesk_user"

    val databasePassword: String =
        System.getenv("DATABASE_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "servicedesk_password"

    val jdbcCredentials: Triple<String, String, String> by lazy {
        Triple(databaseUrl, databaseUser, databasePassword)
    }
}
