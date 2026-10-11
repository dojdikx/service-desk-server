package com.servicedesk.server.messages

data class StaffOutboundPayload(
    val ticketId: Int,
    val messageId: Int,
    val text: String,
    val media: MessageMediaFields = MessageMediaFields(),
)
