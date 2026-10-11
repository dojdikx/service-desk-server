package com.servicedesk.server.realtime

import com.servicedesk.server.auth.JwtService
import com.servicedesk.server.plugins.StaffPrincipal
import com.servicedesk.server.tickets.TicketRepository
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
fun Application.configureRealtimeWebSockets(
    broadcaster: RealtimeBroadcaster,
    ticketRepository: TicketRepository,
) {
    routing {
        webSocket("/api/ws/tickets/{ticketId}") {
            val principal = call.request.queryParameters["token"]?.let { JwtService.principalFromToken(it) }
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "unauthorized"))
            val ticketId = call.parameters["ticketId"]?.toIntOrNull()
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "bad_ticket"))
            if (!canAccessTicket(ticketRepository, ticketId, principal)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "forbidden"))
                return@webSocket
            }
            broadcaster.registerTicket(ticketId, this)
            try {
                send(Frame.Text("""{"type":"connected","ticketId":$ticketId}"""))
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.equals("ping", ignoreCase = true)) {
                            send(Frame.Text("pong"))
                        }
                    }
                }
            } finally {
                broadcaster.unregisterTicket(ticketId, this)
            }
        }

        webSocket("/api/ws/staff") {
            val principal = call.request.queryParameters["token"]?.let { JwtService.principalFromToken(it) }
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "unauthorized"))
            broadcaster.registerStaff(principal.staffId, principal.role, this)
            try {
                send(Frame.Text("""{"type":"connected","scope":"staff"}"""))
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.equals("ping", ignoreCase = true)) {
                            send(Frame.Text("pong"))
                        }
                    }
                }
            } finally {
                broadcaster.unregisterStaff(principal.staffId, this)
            }
        }
    }
}

private suspend fun canAccessTicket(
    ticketRepository: TicketRepository,
    ticketId: Int,
    principal: StaffPrincipal,
): Boolean {
    ticketRepository.findById(ticketId) ?: return false
    if (principal.role == "admin") return true
    return ticketRepository.isAssignedTo(ticketId, principal.staffId)
}
