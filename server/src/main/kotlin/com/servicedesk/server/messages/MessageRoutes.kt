package com.servicedesk.server.messages

import com.servicedesk.server.plugins.StaffPrincipal
import com.servicedesk.server.tickets.StaffSession
import com.servicedesk.server.tickets.TicketAccessDeniedException
import com.servicedesk.server.tickets.TicketNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureMessageRoutes(
    previewUseCase: GetTicketMessagesPreviewUseCase,
    beforeUseCase: GetTicketMessagesBeforeUseCase,
    sendUseCase: SendTicketMessageUseCase,
    retryUseCase: RetryMessageUseCase,
    dialogsUseCase: ListTicketDialogsUseCase,
) {
    routing {
        authenticate("auth-jwt") {
            route("/api/messages") {
                get("/tickets") {
                    val session = call.staffSession() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    call.respond(dialogsUseCase.execute())
                }
                post("/send") {
                    val session = call.staffSession() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    try {
                        val body = call.receive<SendMessageRequest>()
                        call.respond(sendUseCase.execute(body, session))
                    } catch (_: TicketNotFoundException) {
                        call.respond(HttpStatusCode.NotFound, mapOf("code" to "TICKET_NOT_FOUND"))
                    } catch (_: TicketAccessDeniedException) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
                    } catch (_: EmptyMessageException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("code" to "EMPTY_MESSAGE"))
                    }
                }
                post("/{messageId}/retry") {
                    val session = call.staffSession() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val messageId = call.parameters["messageId"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    try {
                        call.respond(retryUseCase.execute(messageId, session))
                    } catch (_: TicketNotFoundException) {
                        call.respond(HttpStatusCode.NotFound, mapOf("code" to "MESSAGE_NOT_FOUND"))
                    } catch (_: TicketAccessDeniedException) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
                    } catch (_: MessageRetryNotAllowedException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("code" to "RETRY_NOT_ALLOWED"))
                    }
                }
                get("/ticket/{ticketId}/preview") {
                    val session = call.staffSession() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val ticketId = call.parameters["ticketId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    try {
                        call.respond(previewUseCase.execute(ticketId, session))
                    } catch (_: TicketNotFoundException) {
                        call.respond(HttpStatusCode.NotFound, mapOf("code" to "TICKET_NOT_FOUND"))
                    } catch (_: TicketAccessDeniedException) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
                    }
                }
                get("/ticket/{ticketId}/history") {
                    val session = call.staffSession() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val ticketId = call.parameters["ticketId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val beforeId = call.request.queryParameters["before_id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("code" to "BEFORE_ID_REQUIRED"))
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 30
                    try {
                        call.respond(beforeUseCase.execute(ticketId, beforeId, limit, session))
                    } catch (_: TicketNotFoundException) {
                        call.respond(HttpStatusCode.NotFound, mapOf("code" to "TICKET_NOT_FOUND"))
                    } catch (_: TicketAccessDeniedException) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
                    }
                }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.staffSession(): StaffSession? {
    val principal = principal<StaffPrincipal>() ?: return null
    return StaffSession(staffId = principal.staffId, role = principal.role)
}
