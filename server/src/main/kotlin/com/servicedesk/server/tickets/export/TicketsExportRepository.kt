package com.servicedesk.server.tickets.export

import com.servicedesk.server.db.TicketsTable
import com.servicedesk.server.tickets.ReplacementPartDto
import com.servicedesk.server.tickets.parseReplacementPartsForApi
import com.servicedesk.server.tickets.parseStringListJson
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.trim
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq

interface TicketsExportRepository {
    suspend fun findForExport(
        startDate: LocalDate,
        endDate: LocalDate,
        channels: List<String>,
    ): List<TicketExportRow>
}

class ExposedTicketsExportRepository : TicketsExportRepository {

    override suspend fun findForExport(
        startDate: LocalDate,
        endDate: LocalDate,
        channels: List<String>,
    ): List<TicketExportRow> = newSuspendedTransaction(Dispatchers.IO) {
        val startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endInstant = endDate.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant()
        val channelScope = channelFilter(channels)

        TicketsTable
            .selectAll()
            .where {
                channelScope and
                    (TicketsTable.createdAt greaterEq startInstant) and
                    (TicketsTable.createdAt lessEq endInstant) and
                    TicketsTable.serviceType.isNotNull() and
                    (TicketsTable.serviceType.trim() neq "")
            }
            .orderBy(TicketsTable.createdAt to org.jetbrains.exposed.sql.SortOrder.ASC)
            .map { it.toExportRow() }
    }

    private fun channelFilter(channels: List<String>): Op<Boolean> {
        if (channels.contains("max")) {
            return TicketsTable.maxUserId.isNotNull()
        }
        return Op.TRUE
    }

    private fun ResultRow.toExportRow(): TicketExportRow {
        val parts = parseReplacementPartsForApi(this[TicketsTable.replacementParts]) ?: emptyList()
        return TicketExportRow(
            id = this[TicketsTable.id],
            serviceType = this[TicketsTable.serviceType] ?: "",
            serviceTime = this[TicketsTable.serviceTime],
            replenishmentRequirement = this[TicketsTable.replenishmentRequirement],
            serialNumber = this[TicketsTable.serialNumber],
            extraSerialNumber = this[TicketsTable.extraSerialNumber],
            purchaseOrderId = this[TicketsTable.purchaseOrderId],
            purchasedFrom = this[TicketsTable.purchasedFrom],
            serviceChannel = parseStringListJson(this[TicketsTable.serviceChannel]),
            serviceSolution = this[TicketsTable.serviceSolution],
            productType = this[TicketsTable.productType],
            printerModel = this[TicketsTable.printerModel],
            warrantyStatus = this[TicketsTable.warrantyStatus],
            reason = this[TicketsTable.reason],
            sku = this[TicketsTable.sku],
            issueType = this[TicketsTable.issueType],
            subIssueType = this[TicketsTable.subIssueType],
            thirdIssueType = this[TicketsTable.thirdIssueType],
            qtyUsed = this[TicketsTable.qtyUsed],
            replacementQty = this[TicketsTable.replacementQty],
            replenishmentQty = this[TicketsTable.replenishmentQty],
            skuRemark = this[TicketsTable.skuRemark],
            replacementParts = parts,
        )
    }
}
