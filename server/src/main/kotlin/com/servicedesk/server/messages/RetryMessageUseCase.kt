package com.servicedesk.server.messages

import com.servicedesk.server.realtime.RealtimeNotifier
import com.servicedesk.server.tickets.StaffSession
import com.servicedesk.server.tickets.TicketAccessDeniedException
import com.servicedesk.server.tickets.TicketNotFoundException
import com.servicedesk.server.tickets.TicketRepository

class RetryMessageUseCase(
    private val ticketRepository: TicketRepository,
    private val messageRepository: MessageRepository,
    private val clientMessengerGateway: ClientMessengerGateway,
    private val realtimeNotifier: RealtimeNotifier,
) {
    suspend fun execute(messageId: Int, session: StaffSession): SendMessageResponse {
        val message = messageRepository.findById(messageId) ?: throw TicketNotFoundException()
        ensureTicketAccess(ticketRepository, message.ticketId, session)
        if (message.messageType != "backend" || message.deliveryStatus != "failed") {
            throw MessageRetryNotAllowedException()
        }
        messageRepository.setDeliveryStatus(messageId, "pending", null)
        realtimeNotifier.onMessageDeliveryUpdated(message.ticketId, messageId, "pending")
        clientMessengerGateway.onStaffMessageSent(
            StaffOutboundPayload(
                ticketId = message.ticketId,
                messageId = messageId,
                text = message.messageText,
                media = MessageMediaFields(
                    mediaType = message.mediaType,
                    mediaUrl = message.mediaUrl,
                    mediaCaption = message.mediaCaption,
                    mediaFileName = message.mediaFileName,
                    mediaItems = message.mediaItems,
                ),
            ),
        )
        return SendMessageResponse(
            status = "pending",
            ticketId = message.ticketId,
            messageId = messageId,
            text = message.messageText,
        )
    }
}

private suspend fun ensureTicketAccess(
    ticketRepository: TicketRepository,
    ticketId: Int,
    session: StaffSession,
) {
    ticketRepository.findById(ticketId) ?: throw TicketNotFoundException()
    if (!session.isAdmin && !ticketRepository.isAssignedTo(ticketId, session.staffId)) {
        throw TicketAccessDeniedException()
    }
}
