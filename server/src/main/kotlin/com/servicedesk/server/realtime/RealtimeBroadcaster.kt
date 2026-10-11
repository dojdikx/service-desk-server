package com.servicedesk.server.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import java.util.concurrent.ConcurrentHashMap

class RealtimeBroadcaster(
    private val realtimeJson: RealtimeJson = RealtimeJson(),
) {
    private val ticketSessions = ConcurrentHashMap<Int, MutableSet<WebSocketSession>>()
    private val staffSessions = ConcurrentHashMap<Int, StaffSocketRegistration>()

    fun registerTicket(ticketId: Int, session: WebSocketSession) {
        ticketSessions.compute(ticketId) { _, set ->
            (set ?: ConcurrentHashMap.newKeySet()).also { it.add(session) }
        }
    }

    fun unregisterTicket(ticketId: Int, session: WebSocketSession) {
        ticketSessions[ticketId]?.remove(session)
    }

    fun registerStaff(staffId: Int, role: String, session: WebSocketSession) {
        staffSessions[staffId] = StaffSocketRegistration(staffId, role, session)
    }

    fun unregisterStaff(staffId: Int, session: WebSocketSession) {
        staffSessions[staffId]?.let { reg ->
            if (reg.session == session) {
                staffSessions.remove(staffId)
            }
        }
    }

    suspend fun broadcastTicket(ticketId: Int, event: RealtimeEvent) {
        val payload = realtimeJson.encode(event)
        ticketSessions[ticketId]?.toList()?.forEach { session ->
            runCatching { session.send(Frame.Text(payload)) }
        }
    }

    suspend fun notifyStaff(
        staffId: Int?,
        isAdminAudience: Boolean,
        event: RealtimeEvent,
    ) {
        val payload = realtimeJson.encode(event)
        staffSessions.values.forEach { reg ->
            val shouldSend = when {
                staffId != null && reg.staffId == staffId -> true
                isAdminAudience && reg.role == "admin" -> true
                else -> false
            }
            if (shouldSend) {
                runCatching { reg.session.send(Frame.Text(payload)) }
            }
        }
    }

    private data class StaffSocketRegistration(
        val staffId: Int,
        val role: String,
        val session: WebSocketSession,
    )
}
