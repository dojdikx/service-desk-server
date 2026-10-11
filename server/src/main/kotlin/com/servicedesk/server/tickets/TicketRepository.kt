package com.servicedesk.server.tickets

import com.servicedesk.server.db.SupportStaffTable
import com.servicedesk.server.db.TicketsTable
import com.servicedesk.server.db.UsersTable
import com.servicedesk.server.util.detectPrinterModelCode
import com.servicedesk.server.util.normalizeSerialNumber
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

interface TicketRepository {
    suspend fun listShort(session: StaffSession): List<TicketShortDto>
    suspend fun findById(ticketId: Int): TicketDetailDto?
    suspend fun create(request: TicketCreateRequest): TicketDetailDto?
    suspend fun patch(ticketId: Int, request: TicketPatchRequest): TicketDetailDto?
    suspend fun assign(ticketId: Int, managerId: Int): TicketDetailDto?
    suspend fun isAssignedTo(ticketId: Int, staffId: Int): Boolean
}

class ExposedTicketRepository : TicketRepository {

    override suspend fun listShort(session: StaffSession): List<TicketShortDto> =
        newSuspendedTransaction(Dispatchers.IO) {
            val query = TicketsTable
                .join(UsersTable, JoinType.INNER, TicketsTable.userId, UsersTable.id)
                .join(SupportStaffTable, JoinType.LEFT, TicketsTable.assignedTo, SupportStaffTable.id)
                .selectAll()

            val rows = if (session.isAdmin) {
                query.orderBy(TicketsTable.createdAt to SortOrder.DESC)
            } else {
                query
                    .where { TicketsTable.assignedTo eq session.staffId }
                    .orderBy(TicketsTable.createdAt to SortOrder.DESC)
            }

            rows.map { row -> row.toShortDto() }
        }

    override suspend fun findById(ticketId: Int): TicketDetailDto? =
        newSuspendedTransaction(Dispatchers.IO) {
            TicketsTable
                .selectAll()
                .where { TicketsTable.id eq ticketId }
                .firstOrNull()
                ?.toDetailDto()
        }

    override suspend fun create(request: TicketCreateRequest): TicketDetailDto? =
        newSuspendedTransaction(Dispatchers.IO) {
            val user = UsersTable
                .selectAll()
                .where { UsersTable.id eq request.userId }
                .firstOrNull()
                ?: return@newSuspendedTransaction null

            val status = request.status?.takeIf { TicketColumnsConfig.isValidStatus(it) }
                ?: TicketColumnsConfig.defaultStatus()
            val serial = request.serialNumber?.let { normalizeSerialNumber(it) }
            val model = request.printerModel
                ?: serial?.let { detectPrinterModelCode(it) }
                ?: user[UsersTable.printerModel]

            val now = Instant.now()
            val id = TicketsTable.insert {
                it[userId] = user[UsersTable.id]
                it[maxUserId] = user[UsersTable.maxId]
                it[TicketsTable.status] = status
                it[tags] = tagsToJson(request.tags)
                it[assignedTo] = request.assignedTo
                it[createdAt] = now
                it[updatedAt] = now
                it[reason] = request.reason
                it[printerModel] = model
                it[serialNumber] = serial ?: user[UsersTable.serialNumber]
                it[fullName] = request.fullName ?: user[UsersTable.name]
                it[contactPhone] = request.contactPhone ?: user[UsersTable.phone]
                it[serviceChannel] = "[]"
                it[replacementParts] = "[]"
            }[TicketsTable.id]

            TicketsTable
                .selectAll()
                .where { TicketsTable.id eq id }
                .firstOrNull()
                ?.toDetailDto()
        }

    override suspend fun patch(ticketId: Int, request: TicketPatchRequest): TicketDetailDto? =
        newSuspendedTransaction(Dispatchers.IO) {
            val exists = TicketsTable.selectAll().where { TicketsTable.id eq ticketId }.any()
            if (!exists) return@newSuspendedTransaction null

            request.status?.let { status ->
                if (!TicketColumnsConfig.isValidStatus(status)) {
                    throw TicketValidationException("Недопустимый статус заявки")
                }
            }

            val serial = request.serialNumber?.let { normalizeSerialNumber(it) }
            val modelFromSerial = serial?.let { detectPrinterModelCode(it) }

            TicketsTable.update({ TicketsTable.id eq ticketId }) { row ->
                request.status?.let { row[TicketsTable.status] = it }
                request.tags?.let { row[tags] = tagsToJson(it) }
                request.reason?.let { row[reason] = it }
                request.printerModel?.let { row[printerModel] = it }
                    ?: modelFromSerial?.let { row[printerModel] = it }
                serial?.let { row[TicketsTable.serialNumber] = it }
                request.trackingNumber?.let { row[trackingNumber] = it }
                request.deliveryAddress?.let { row[deliveryAddress] = it }
                request.warrantyCase?.let { row[warrantyCase] = it }
                request.purchasedFrom?.let { row[purchasedFrom] = it }
                request.sku?.let { row[sku] = it }
                request.deliveryCost?.let { row[deliveryCost] = it.toBigDecimal() }
                request.solutionDescription?.let { row[solutionDescription] = it }
                request.fullName?.let { row[fullName] = it }
                request.contactPhone?.let { row[contactPhone] = it }
                request.externalExportUrl?.let { row[externalExportUrl] = it }
                request.assignedTo?.let { row[assignedTo] = it }
            }

            TicketsTable
                .selectAll()
                .where { TicketsTable.id eq ticketId }
                .firstOrNull()
                ?.toDetailDto()
        }

    override suspend fun assign(ticketId: Int, managerId: Int): TicketDetailDto? =
        newSuspendedTransaction(Dispatchers.IO) {
            val managerExists = SupportStaffTable
                .selectAll()
                .where { (SupportStaffTable.id eq managerId) and (SupportStaffTable.isActive eq true) }
                .any()
            if (!managerExists) return@newSuspendedTransaction null

            val updated = TicketsTable.update({ TicketsTable.id eq ticketId }) {
                it[assignedTo] = managerId
            }
            if (updated == 0) return@newSuspendedTransaction null
            TicketsTable
                .selectAll()
                .where { TicketsTable.id eq ticketId }
                .firstOrNull()
                ?.toDetailDto()
        }

    override suspend fun isAssignedTo(ticketId: Int, staffId: Int): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            TicketsTable
                .selectAll()
                .where { TicketsTable.id eq ticketId and (TicketsTable.assignedTo eq staffId) }
                .any()
        }

    private fun ResultRow.toShortDto(): TicketShortDto {
        val assigneeFirst = this.getOrNull(SupportStaffTable.firstName)
        val assigneeLast = this.getOrNull(SupportStaffTable.lastName)
        val assigneeName = listOfNotNull(assigneeFirst, assigneeLast)
            .joinToString(" ")
            .trim()
            .ifBlank { null }
        val clientName = this[TicketsTable.fullName]?.trim()?.takeIf { it.isNotEmpty() }
            ?: this[UsersTable.name]
        return TicketShortDto(
            ticketId = this[TicketsTable.id],
            userId = this[TicketsTable.userId],
            tags = parseTagsJson(this[TicketsTable.tags]),
            createdAt = this[TicketsTable.createdAt].toIsoString(),
            clientName = clientName,
            clientAvatarUrl = this[UsersTable.avatarUrl],
            lastMessage = null,
            status = this[TicketsTable.status],
            assignedTo = this[TicketsTable.assignedTo],
            assigneeName = assigneeName,
            unreadCount = 0,
            serialNumber = this[TicketsTable.serialNumber],
        )
    }

    private fun ResultRow.toDetailDto(): TicketDetailDto {
        val created = this[TicketsTable.createdAt]
        val updated = this[TicketsTable.updatedAt]
        return TicketDetailDto(
            id = this[TicketsTable.id],
            userId = this[TicketsTable.userId],
            status = this[TicketsTable.status],
            tags = parseTagsJson(this[TicketsTable.tags]),
            createdAt = created.toIsoString(),
            updatedAt = updated.toIsoString(),
            reason = this[TicketsTable.reason],
            printerModel = this[TicketsTable.printerModel],
            serialNumber = this[TicketsTable.serialNumber],
            trackingNumber = this[TicketsTable.trackingNumber],
            deliveryAddress = this[TicketsTable.deliveryAddress],
            warrantyCase = this[TicketsTable.warrantyCase],
            purchasedFrom = this[TicketsTable.purchasedFrom],
            sku = this[TicketsTable.sku],
            deliveryCost = this[TicketsTable.deliveryCost]?.toDouble(),
            solutionDescription = this[TicketsTable.solutionDescription],
            fullName = this[TicketsTable.fullName],
            contactPhone = this[TicketsTable.contactPhone],
            externalExportUrl = this[TicketsTable.externalExportUrl],
            assignedTo = this[TicketsTable.assignedTo],
            maxUserId = this[TicketsTable.maxUserId],
        )
    }
}

class TicketValidationException(message: String) : Exception(message)

private fun Instant.toIsoString(): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.atOffset(ZoneOffset.UTC))

