package com.servicedesk.server.messages

import kotlinx.serialization.Serializable

@Serializable
data class MediaItemDto(
    val mediaType: String,
    val mediaUrl: String,
    val mediaFileName: String? = null,
    val mediaCaption: String? = null,
)

@Serializable
data class SendMessageRequest(
    val ticketId: Int,
    val text: String = "",
    val replyToId: Int? = null,
    val quoteText: String? = null,
    val quoteOffset: Int? = null,
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val mediaCaption: String? = null,
    val mediaFileName: String? = null,
    val mediaItems: List<MediaItemDto>? = null,
)

@Serializable
data class SendMessageResponse(
    val status: String,
    val ticketId: Int,
    val messageId: Int,
    val text: String,
)

@Serializable
data class MessageDto(
    val id: Int,
    val ticketId: Int,
    val messageText: String,
    val messageType: String,
    val timestamp: String,
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val mediaCaption: String? = null,
    val mediaFileName: String? = null,
    val mediaItems: List<MediaItemDto>? = null,
    val deliveryStatus: String? = null,
    val deliveryError: String? = null,
    val replyToId: Int? = null,
    val quoteText: String? = null,
    val quoteOffset: Int? = null,
)

@Serializable
data class TicketMessagesResponse(
    val ticketId: Int,
    val messages: List<MessageDto>,
)

@Serializable
data class TicketDialogItemDto(
    val ticketId: Int,
)

@Serializable
data class TicketDialogsResponse(
    val tickets: List<TicketDialogItemDto>,
)
