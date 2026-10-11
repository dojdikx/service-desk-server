package com.servicedesk.server.realtime

import com.servicedesk.server.messages.MessageDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RealtimeEvent(
    val type: String,
    val ticketId: Int,
    val message: MessageDto? = null,
    val messageId: Int? = null,
    val deliveryStatus: String? = null,
    val unreadCount: Int? = null,
)

object RealtimeEventTypes {
    const val MESSAGE_NEW = "message.new"
    const val MESSAGE_DELIVERY = "message.delivery"
    const val DIALOG_ACTIVITY = "dialog.activity"
}

class RealtimeJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(event: RealtimeEvent): String = json.encodeToString(event)
}
