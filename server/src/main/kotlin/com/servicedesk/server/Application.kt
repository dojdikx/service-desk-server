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
import com.servicedesk.server.messages.ExposedMessageRepository
import com.servicedesk.server.messages.GetTicketMessagesBeforeUseCase
import com.servicedesk.server.messages.GetTicketMessagesPreviewUseCase
import com.servicedesk.server.messages.ListTicketDialogsUseCase
import com.servicedesk.server.messages.MarkTicketReadUseCase
import com.servicedesk.server.messages.SendTicketMessageUseCase
import com.servicedesk.server.messages.configureMessageRoutes
import com.servicedesk.server.staff.ExposedStaffDirectoryRepository
import com.servicedesk.server.staff.configureStaffDirectoryRoutes
import com.servicedesk.server.tickets.AssignTicketUseCase
import com.servicedesk.server.tickets.CreateTicketUseCase
import com.servicedesk.server.tickets.ExposedTicketRepository
import com.servicedesk.server.tickets.FilterTicketsUseCase
import com.servicedesk.server.tickets.GetTicketUseCase
import com.servicedesk.server.tickets.ListTicketsUseCase
import com.servicedesk.server.tickets.PatchTicketUseCase
import com.servicedesk.server.tickets.configureTicketRoutes
import com.servicedesk.server.users.ExposedUserRepository
import com.servicedesk.server.users.configureUserRoutes
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

    val messageRepository = ExposedMessageRepository()
    val ticketRepository = ExposedTicketRepository(messageRepository)
    configureTicketRoutes(
        listTicketsUseCase = ListTicketsUseCase(ticketRepository),
        filterTicketsUseCase = FilterTicketsUseCase(ticketRepository),
        getTicketUseCase = GetTicketUseCase(ticketRepository),
        createTicketUseCase = CreateTicketUseCase(ticketRepository),
        patchTicketUseCase = PatchTicketUseCase(ticketRepository),
        assignTicketUseCase = AssignTicketUseCase(ticketRepository),
        markTicketReadUseCase = MarkTicketReadUseCase(ticketRepository, messageRepository),
    )
    configureMessageRoutes(
        previewUseCase = GetTicketMessagesPreviewUseCase(ticketRepository, messageRepository),
        beforeUseCase = GetTicketMessagesBeforeUseCase(ticketRepository, messageRepository),
        sendUseCase = SendTicketMessageUseCase(ticketRepository, messageRepository),
        dialogsUseCase = ListTicketDialogsUseCase(messageRepository),
    )

    configureUserRoutes(ExposedUserRepository())
    configureStaffDirectoryRoutes(ExposedStaffDirectoryRepository())

    configureMonitoring()
    configureRouting()
}
