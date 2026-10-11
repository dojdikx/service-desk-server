package com.servicedesk.server.analytics

import com.servicedesk.server.db.AnalyticsTable
import com.servicedesk.server.db.SupportStaffTable
import com.servicedesk.server.db.TicketsTable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

data class AnalyticsQuery(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val channels: List<String>,
    val includeStatusBreakdown: Boolean,
    val includePrinterModelsBreakdown: Boolean,
    val includeTicketsBreakdown: Boolean,
)

data class ManagerEfficiencyRow(
    val staffId: Int,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val totalTickets: Int,
    val openTickets: Int,
    val closedTickets: Int,
)

interface AnalyticsRepository {
    suspend fun compute(query: AnalyticsQuery): AnalyticsData
    suspend fun saveReportPath(path: String)
}

data class AnalyticsData(
    val period: AnalyticsPeriodDto,
    val ticketsCreated: Int,
    val ticketsModified: Int,
    val statusBreakdown: Map<String, Int> = emptyMap(),
    val printerModelBreakdown: Map<String, Int> = emptyMap(),
    val ticketsBreakdown: List<AnalyticsTicketRowDto> = emptyList(),
    val managerEfficiency: List<ManagerEfficiencyRow> = emptyList(),
)

class ExposedAnalyticsRepository : AnalyticsRepository {

    override suspend fun compute(query: AnalyticsQuery): AnalyticsData =
        newSuspendedTransaction(Dispatchers.IO) {
            val channelScope = channelFilter(query.channels)
            val startInstant = query.startDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            val endInstant = query.endDate.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant()
            val endDayInstant = query.endDate.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant()

            val created = TicketsTable
                .selectAll()
                .where {
                    channelScope and
                        (TicketsTable.createdAt greaterEq startInstant) and
                        (TicketsTable.createdAt lessEq endInstant)
                }
                .count()
                .toInt()

            val modified = TicketsTable
                .selectAll()
                .where {
                    channelScope and
                        (TicketsTable.updatedAt greaterEq startInstant) and
                        (TicketsTable.updatedAt lessEq endInstant) and
                        (TicketsTable.updatedAt greater TicketsTable.createdAt)
                }
                .count()
                .toInt()

            val existsBeforeEnd = channelScope and (TicketsTable.createdAt lessEq endDayInstant)

            val statusMap = if (query.includeStatusBreakdown) {
                TicketsTable
                    .selectAll()
                    .where { existsBeforeEnd }
                    .map { it[TicketsTable.status] }
                    .groupingBy { it }
                    .eachCount()
            } else {
                emptyMap()
            }

            val printerMap = if (query.includePrinterModelsBreakdown) {
                TicketsTable
                    .selectAll()
                    .where {
                        existsBeforeEnd and TicketsTable.printerModel.isNotNull()
                    }
                    .mapNotNull { it[TicketsTable.printerModel]?.trim()?.takeIf { it.isNotEmpty() } }
                    .groupingBy { it }
                    .eachCount()
            } else {
                emptyMap()
            }

            val ticketRows = if (query.includeTicketsBreakdown) {
                TicketsTable
                    .selectAll()
                    .where { existsBeforeEnd }
                    .orderBy(TicketsTable.createdAt to SortOrder.ASC)
                    .map { it.toAnalyticsRow() }
            } else {
                emptyList()
            }

            val managers = managerEfficiency(existsBeforeEnd)

            AnalyticsData(
                period = AnalyticsPeriodDto(
                    startDate = query.startDate.toString(),
                    endDate = query.endDate.toString(),
                    channels = query.channels,
                ),
                ticketsCreated = created,
                ticketsModified = modified,
                statusBreakdown = statusMap,
                printerModelBreakdown = printerMap,
                ticketsBreakdown = ticketRows,
                managerEfficiency = managers,
            )
        }

    override suspend fun saveReportPath(path: String) {
        newSuspendedTransaction(Dispatchers.IO) {
            AnalyticsTable.insert {
                it[AnalyticsTable.path] = path
                it[createdAt] = Instant.now()
            }
        }
    }

    private fun channelFilter(channels: List<String>): Op<Boolean> {
        if (channels.contains("max")) {
            return TicketsTable.maxUserId.isNotNull()
        }
        return Op.TRUE
    }

    private fun managerEfficiency(scope: Op<Boolean>): List<ManagerEfficiencyRow> {
        val rows = TicketsTable
            .join(SupportStaffTable, JoinType.INNER, TicketsTable.assignedTo, SupportStaffTable.id)
            .selectAll()
            .where { scope and TicketsTable.assignedTo.isNotNull() }
            .toList()
        return rows
            .groupBy { it[SupportStaffTable.id] }
            .map { (_, ticketRows) ->
                val first = ticketRows.first()
                val total = ticketRows.size
                val closed = ticketRows.count { CLOSED_STATUSES.contains(it[TicketsTable.status]) }
                ManagerEfficiencyRow(
                    staffId = first[SupportStaffTable.id],
                    email = first[SupportStaffTable.email],
                    firstName = first[SupportStaffTable.firstName],
                    lastName = first[SupportStaffTable.lastName],
                    totalTickets = total,
                    openTickets = total - closed,
                    closedTickets = closed,
                )
            }
            .sortedByDescending { it.totalTickets }
    }

    private fun ResultRow.toAnalyticsRow(): AnalyticsTicketRowDto {
        val created = this[TicketsTable.createdAt]
        val updated = this[TicketsTable.updatedAt]
        return AnalyticsTicketRowDto(
            id = this[TicketsTable.id],
            status = this[TicketsTable.status],
            createdAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(created.atOffset(ZoneOffset.UTC)),
            updatedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(updated.atOffset(ZoneOffset.UTC)),
            printerModel = this[TicketsTable.printerModel],
            serialNumber = this[TicketsTable.serialNumber],
            assignedTo = this[TicketsTable.assignedTo],
            reason = this[TicketsTable.reason],
        )
    }

    companion object {
        private val CLOSED_STATUSES = setOf(
            "Завершенные",
            "Закрытые",
            "Успешно реализовано",
            "Отказ",
        )
    }
}
