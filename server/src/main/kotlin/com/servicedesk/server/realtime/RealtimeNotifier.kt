package com.servicedesk.server.realtime

import com.servicedesk.server.messages.MessageDto
import com.servicedesk.server.messages.MessageRepository
import com.servicedesk.server.tickets.TicketRepository

class RealtimeNotifier(
    private val broadcaster: RealtimeBroadcaster,
    private val ticketRepository: TicketRepository,
    private val messageRepository: MessageRepository,
) {
    suspend fun onMessageCreated(ticketId: Int, message: MessageDto) {
        broadcaster.broadcastTicket(
            ticketId,
            RealtimeEvent(
                type = RealtimeEventTypes.MESSAGE_NEW,
                ticketId = ticketId,
                message = message,
            ),
        )
        publishDialogActivity(ticketId)
    }

    suspend fun onMessageDeliveryUpdated(
        ticketId: Int,
        messageId: Int,
        deliveryStatus: String,
        deliveryError: String? = null,
    ) {
        broadcaster.broadcastTicket(
            ticketId,
            RealtimeEvent(
                type = RealtimeEventTypes.MESSAGE_DELIVERY,
                ticketId = ticketId,
                messageId = messageId,
                deliveryStatus = deliveryStatus,
                deliveryError = deliveryError,
            ),
        )
    }

    private suspend fun publishDialogActivity(ticketId: Int) {
        val ticket = ticketRepository.findById(ticketId) ?: return
        val assignee = ticket.assignedTo
        val unread = assignee?.let { messageRepository.unreadCount(it, ticketId) }
        val event = RealtimeEvent(
            type = RealtimeEventTypes.DIALOG_ACTIVITY,
            ticketId = ticketId,
            unreadCount = unread,
        )
        broadcaster.notifyStaff(
            staffId = assignee,
            isAdminAudience = assignee == null,
            event = event,
        )
    }
}
