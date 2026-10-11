package com.servicedesk.server.messages

import com.servicedesk.server.tickets.StaffSession
import com.servicedesk.server.tickets.TicketAccessDeniedException
import com.servicedesk.server.tickets.TicketNotFoundException
import com.servicedesk.server.tickets.TicketRepository

class GetTicketMessagesPreviewUseCase(
    private val ticketRepository: TicketRepository,
    private val messageRepository: MessageRepository,
) {
    suspend fun execute(ticketId: Int, session: StaffSession): TicketMessagesResponse {
        ensureTicketAccess(ticketRepository, ticketId, session)
        return TicketMessagesResponse(
            ticketId = ticketId,
            messages = messageRepository.listPreview(ticketId),
        )
    }
}

class GetTicketMessagesBeforeUseCase(
    private val ticketRepository: TicketRepository,
    private val messageRepository: MessageRepository,
) {
    suspend fun execute(ticketId: Int, beforeId: Int, limit: Int, session: StaffSession): TicketMessagesResponse {
        ensureTicketAccess(ticketRepository, ticketId, session)
        return TicketMessagesResponse(
            ticketId = ticketId,
            messages = messageRepository.listBefore(ticketId, beforeId, limit),
        )
    }
}

class SendTicketMessageUseCase(
    private val ticketRepository: TicketRepository,
    private val messageRepository: MessageRepository,
) {
    suspend fun execute(request: SendMessageRequest, session: StaffSession): SendMessageResponse {
        ensureTicketAccess(ticketRepository, request.ticketId, session)
        val message = messageRepository.sendBackendMessage(
            ticketId = request.ticketId,
            text = request.text.trim(),
            replyToId = request.replyToId,
            quoteText = request.quoteText,
            quoteOffset = request.quoteOffset,
        ) ?: throw TicketNotFoundException()
        return SendMessageResponse(
            status = "pending",
            ticketId = request.ticketId,
            messageId = message.id,
            text = message.messageText,
        )
    }
}

class MarkTicketReadUseCase(
    private val ticketRepository: TicketRepository,
    private val messageRepository: MessageRepository,
) {
    suspend fun execute(ticketId: Int, session: StaffSession) {
        ensureTicketAccess(ticketRepository, ticketId, session)
        messageRepository.markTicketRead(session.staffId, ticketId)
    }
}

class ListTicketDialogsUseCase(
    private val messageRepository: MessageRepository,
) {
    suspend fun execute(): TicketDialogsResponse =
        TicketDialogsResponse(
            tickets = messageRepository.ticketsWithMessages().map { TicketDialogItemDto(it) },
        )
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
