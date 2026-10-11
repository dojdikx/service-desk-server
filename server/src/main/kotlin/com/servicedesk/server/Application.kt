package com.servicedesk.server

import com.servicedesk.server.config.AppConfig
import com.servicedesk.server.db.DatabaseFactory
import com.servicedesk.server.auth.ExposedStaffAuthRepository
import com.servicedesk.server.auth.GetCurrentStaffUseCase
import com.servicedesk.server.auth.LoginStaffUseCase
import com.servicedesk.server.auth.RegisterStaffUseCase
import com.servicedesk.server.plugins.configureAuth
import com.servicedesk.server.plugins.configureFlyway
import com.servicedesk.server.plugins.configureMonitoring
import com.servicedesk.server.plugins.configureRouting
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import kotlinx.serialization.json.Json

fun main() {
    val server = embeddedServer(
        Netty,
        port = AppConfig.port,
        host = AppConfig.host,
        module = Application::module,
    )
    server.start(wait = true)
    runCatching { DatabaseFactory.close() }
}

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }
    install(CORS) {
        allowHost("localhost")
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    configureFlyway()
    DatabaseFactory.init()

    val staffAuthRepository = ExposedStaffAuthRepository()
    configureAuth(
        registerStaffUseCase = RegisterStaffUseCase(staffAuthRepository),
        loginStaffUseCase = LoginStaffUseCase(staffAuthRepository),
        getCurrentStaffUseCase = GetCurrentStaffUseCase(staffAuthRepository),
    )

    configureMonitoring()
    configureRouting()
}
