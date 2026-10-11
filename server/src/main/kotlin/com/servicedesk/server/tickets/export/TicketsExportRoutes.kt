package com.servicedesk.server.tickets.export

import com.servicedesk.server.plugins.StaffPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureTicketsExportRoutes(
    exportTicketsUseCase: ExportTicketsUseCase,
) {
    routing {
        authenticate("auth-jwt") {
            route("/api/tickets/export") {
                post {
                    if (call.principal<StaffPrincipal>() == null) {
                        return@post call.respond(HttpStatusCode.Unauthorized)
                    }
                    try {
                        val body = call.receive<TicketsExportRequest>()
                        call.respond(exportTicketsUseCase.execute(body))
                    } catch (_: InvalidTicketsExportDateRangeException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("code" to "INVALID_DATE_RANGE"))
                    } catch (_: TicketsExportChannelsRequiredException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("code" to "CHANNELS_REQUIRED"))
                    }
                }
            }
        }
    }
}
