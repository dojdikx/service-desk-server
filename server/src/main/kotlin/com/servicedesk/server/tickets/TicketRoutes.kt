package com.servicedesk.server.tickets

import com.servicedesk.server.plugins.StaffPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import java.time.LocalDate
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureTicketRoutes(
    listTicketsUseCase: ListTicketsUseCase,
    filterTicketsUseCase: FilterTicketsUseCase,
    getTicketUseCase: GetTicketUseCase,
    createTicketUseCase: CreateTicketUseCase,
    patchTicketUseCase: PatchTicketUseCase,
    assignTicketUseCase: AssignTicketUseCase,
    markTicketReadUseCase: com.servicedesk.server.messages.MarkTicketReadUseCase,
) {
    routing {
        authenticate("auth-jwt") {
            route("/api/tickets") {
                ticketRoutes(
                    listTicketsUseCase,
                    filterTicketsUseCase,
                    getTicketUseCase,
                    createTicketUseCase,
                    patchTicketUseCase,
                    assignTicketUseCase,
                    markTicketReadUseCase,
                )
            }
        }
    }
}

private fun Route.ticketRoutes(
    listTicketsUseCase: ListTicketsUseCase,
    filterTicketsUseCase: FilterTicketsUseCase,
    getTicketUseCase: GetTicketUseCase,
    createTicketUseCase: CreateTicketUseCase,
    patchTicketUseCase: PatchTicketUseCase,
    assignTicketUseCase: AssignTicketUseCase,
    markTicketReadUseCase: com.servicedesk.server.messages.MarkTicketReadUseCase,
) {
    get("/columns") {
        call.respond(TicketColumnsConfig.columnsResponse())
    }

    get("/issue-types") {
        if (call.staffSession() == null) return@get call.respond(HttpStatusCode.Unauthorized)
        val lang = call.request.queryParameters["lang"] ?: "ru"
        call.respond(IssueTypesConfig.response(lang))
    }

    get("/filter") {
        val session = call.staffSession() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val startDate = call.request.queryParameters["start_date"]?.let { LocalDate.parse(it) }
        val endDate = call.request.queryParameters["end_date"]?.let { LocalDate.parse(it) }
        val tagStr = call.request.queryParameters["tag_str"]
        val serialNumber = call.request.queryParameters["serial_number"]
        val manager = call.request.queryParameters["manager"]?.toIntOrNull()
        try {
            call.respond(
                filterTicketsUseCase.execute(
                    session,
                    TicketFilterParams(
                        startDate = startDate,
                        endDate = endDate,
                        tagStr = tagStr,
                        serialNumber = serialNumber,
                        manager = manager,
                    ),
                ),
            )
        } catch (_: FiltersRequiredException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "message" to "Укажите хотя бы один параметр фильтрации",
                    "code" to "FILTERS_REQUIRED",
                ),
            )
        } catch (_: InvalidDateRangeException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "message" to "Дата начала не может быть позже даты окончания",
                    "code" to "INVALID_DATE_RANGE",
                ),
            )
        } catch (_: TicketAccessDeniedException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
        }
    }

    get {
        val session = call.staffSession() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        call.respond(listTicketsUseCase.execute(session))
    }

    post {
        val session = call.staffSession() ?: return@post call.respond(HttpStatusCode.Unauthorized)
        try {
            val body = call.receive<TicketCreateRequest>()
            val created = createTicketUseCase.execute(body, session)
            call.respond(HttpStatusCode.Created, created)
        } catch (e: TicketValidationException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to (e.message ?: ""), "code" to "VALIDATION_ERROR"),
            )
        } catch (_: TicketAccessDeniedException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
        }
    }

    get("/{ticketId}") {
        val session = call.staffSession() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val id = call.parameters["ticketId"]?.toIntOrNull()
            ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Некорректный ID", "code" to "INVALID_ID"),
            )
        try {
            call.respond(getTicketUseCase.execute(id, session))
        } catch (_: TicketNotFoundException) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("message" to "Заявка не найдена", "code" to "TICKET_NOT_FOUND"),
            )
        } catch (_: TicketAccessDeniedException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
        }
    }

    patch("/{ticketId}") {
        val session = call.staffSession() ?: return@patch call.respond(HttpStatusCode.Unauthorized)
        val id = call.parameters["ticketId"]?.toIntOrNull()
            ?: return@patch call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Некорректный ID", "code" to "INVALID_ID"),
            )
        try {
            val body = call.receive<TicketPatchRequest>()
            call.respond(patchTicketUseCase.execute(id, body, session))
        } catch (e: TicketValidationException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to (e.message ?: ""), "code" to "VALIDATION_ERROR"),
            )
        } catch (_: TicketNotFoundException) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("message" to "Заявка не найдена", "code" to "TICKET_NOT_FOUND"),
            )
        } catch (_: TicketAccessDeniedException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
        }
    }

    post("/{ticketId}/read") {
        val session = call.staffSession() ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val id = call.parameters["ticketId"]?.toIntOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("code" to "INVALID_ID"))
        try {
            markTicketReadUseCase.execute(id, session)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        } catch (_: TicketNotFoundException) {
            call.respond(HttpStatusCode.NotFound, mapOf("code" to "TICKET_NOT_FOUND"))
        } catch (_: TicketAccessDeniedException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
        }
    }

    post("/{ticketId}/assign") {
        val session = call.staffSession() ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val id = call.parameters["ticketId"]?.toIntOrNull()
            ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Некорректный ID", "code" to "INVALID_ID"),
            )
        try {
            val body = call.receive<AssignTicketRequest>()
            call.respond(assignTicketUseCase.execute(id, body.managerId, session))
        } catch (_: TicketNotFoundException) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("message" to "Заявка не найдена", "code" to "TICKET_NOT_FOUND"),
            )
        } catch (_: TicketAccessDeniedException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.staffSession(): StaffSession? {
    val principal = principal<StaffPrincipal>() ?: return null
    return StaffSession(staffId = principal.staffId, role = principal.role)
}
