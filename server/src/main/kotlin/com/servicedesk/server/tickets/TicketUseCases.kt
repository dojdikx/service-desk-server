package com.servicedesk.server.tickets

class ListTicketsUseCase(
    private val repository: TicketRepository,
) {
    suspend fun execute(session: StaffSession): TicketListResponse =
        TicketListResponse(tickets = repository.listShort(session))
}

class GetTicketUseCase(
    private val repository: TicketRepository,
) {
    suspend fun execute(ticketId: Int, session: StaffSession): TicketDetailDto {
        val ticket = repository.findById(ticketId) ?: throw TicketNotFoundException()
        if (!session.isAdmin && !repository.isAssignedTo(ticketId, session.staffId)) {
            throw TicketAccessDeniedException()
        }
        return ticket
    }
}

class CreateTicketUseCase(
    private val repository: TicketRepository,
) {
    suspend fun execute(request: TicketCreateRequest, session: StaffSession): TicketDetailDto {
        if (request.userId <= 0) {
            throw TicketValidationException("Не указан user_id")
        }
        val created = repository.create(request) ?: throw TicketValidationException("Не удалось создать заявку")
        if (!session.isAdmin && request.assignedTo != null && request.assignedTo != session.staffId) {
            throw TicketAccessDeniedException()
        }
        return created
    }
}

class PatchTicketUseCase(
    private val repository: TicketRepository,
) {
    suspend fun execute(ticketId: Int, request: TicketPatchRequest, session: StaffSession): TicketDetailDto {
        if (!session.isAdmin && !repository.isAssignedTo(ticketId, session.staffId)) {
            throw TicketAccessDeniedException()
        }
        if (!session.isAdmin && request.assignedTo != null) {
            throw TicketAccessDeniedException()
        }
        return try {
            repository.patch(ticketId, request) ?: throw TicketNotFoundException()
        } catch (e: TicketValidationException) {
            throw e
        }
    }
}

class AssignTicketUseCase(
    private val repository: TicketRepository,
) {
    suspend fun execute(ticketId: Int, managerId: Int, session: StaffSession): TicketDetailDto {
        if (!session.isAdmin) throw TicketAccessDeniedException()
        return repository.assign(ticketId, managerId) ?: throw TicketNotFoundException()
    }
}

class TicketNotFoundException : Exception("Заявка не найдена")
class TicketAccessDeniedException : Exception("Недостаточно прав")
