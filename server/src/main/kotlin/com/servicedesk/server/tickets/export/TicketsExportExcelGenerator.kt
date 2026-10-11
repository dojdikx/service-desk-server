package com.servicedesk.server.tickets.export

import com.servicedesk.server.tickets.ReplacementPartDto
import java.nio.file.Path
import java.time.LocalDate
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object TicketsExportExcelGenerator {

    private val headers = listOf(
        "Service Type",
        "Service Time",
        "Replenishment Requirement",
        "SN",
        "Order ID",
        "Purchase Channel",
        "Service Channel",
        "Service Solution",
        "Product Type",
        "Warranty Status",
        "Service Order Remark",
        "SKU",
        "Issue Type",
        "Sub Issue Type",
        "Third-Level Issue Type",
        "Qty used",
        "Replacement Qty",
        "Replenishment Qty",
        "SKU Remark",
    )

    fun write(tickets: List<TicketExportRow>, target: Path) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("BatchImport")
            sheet.createRow(0).apply {
                headers.forEachIndexed { index, title ->
                    createCell(index).setCellValue(title)
                }
            }
            var rowIndex = 1
            tickets.forEach { ticket ->
                rowsForTicket(ticket).forEach { cells ->
                    sheet.createRow(rowIndex++).apply {
                        cells.forEachIndexed { col, value ->
                            createCell(col).setCellValue(value)
                        }
                    }
                }
            }
            target.toFile().outputStream().use { out ->
                workbook.write(out)
            }
        }
    }

    private fun rowsForTicket(ticket: TicketExportRow): List<List<String>> {
        val base = baseFields(ticket)
        val parts = buildList {
            val mainHasPartData = listOfNotNull(
                ticket.sku,
                ticket.qtyUsed,
                ticket.replacementQty,
                ticket.replenishmentQty,
                ticket.skuRemark,
            ).isNotEmpty()
            if (mainHasPartData) {
                add(PartSlice(ticket.sku, ticket.qtyUsed, ticket.replacementQty, ticket.replenishmentQty, ticket.skuRemark))
            }
            ticket.replacementParts.forEach { part ->
                add(
                    PartSlice(
                        sku = part.sku,
                        qtyUsed = null,
                        replacementQty = part.qty,
                        replenishmentQty = null,
                        skuRemark = part.remark,
                    ),
                )
            }
        }
        if (parts.isEmpty()) {
            return listOf(base + partCells(ticket, null))
        }
        return parts.map { slice -> base + partCellsFromSlice(ticket, slice) }
    }

    private data class PartSlice(
        val sku: String?,
        val qtyUsed: Int?,
        val replacementQty: Int?,
        val replenishmentQty: Int?,
        val skuRemark: String?,
    )

    private fun baseFields(ticket: TicketExportRow): List<String> = listOf(
        formatServiceType(ticket.serviceType),
        formatServiceTime(ticket.serviceTime),
        ticket.replenishmentRequirement.orEmpty(),
        formatSerialNumbers(ticket.serialNumber, ticket.extraSerialNumber),
        formatOrderId(ticket.purchaseOrderId),
        normalizePurchaseChannel(ticket.purchasedFrom),
        ticket.serviceChannel.joinToString(", "),
        ticket.serviceSolution.orEmpty(),
        resolveProductType(ticket.productType, ticket.printerModel),
        normalizeWarrantyStatus(ticket.warrantyStatus),
        ticket.reason.orEmpty(),
    )

    private fun partCells(ticket: TicketExportRow, slice: PartSlice?): List<String> {
        val part = slice ?: PartSlice(
            sku = ticket.sku,
            qtyUsed = ticket.qtyUsed,
            replacementQty = ticket.replacementQty,
            replenishmentQty = ticket.replenishmentQty,
            skuRemark = ticket.skuRemark,
        )
        return partCellsFromSlice(ticket, part)
    }

    private fun partCellsFromSlice(ticket: TicketExportRow, slice: PartSlice): List<String> = listOf(
        slice.sku.orEmpty(),
        ticket.issueType.orEmpty(),
        ticket.subIssueType.orEmpty(),
        ticket.thirdIssueType.orEmpty(),
        slice.qtyUsed?.toString() ?: "",
        slice.replacementQty?.toString() ?: "",
        slice.replenishmentQty?.toString() ?: "",
        slice.skuRemark.orEmpty(),
    )

    private fun formatServiceType(raw: String): String =
        when (raw.lowercase()) {
            "repair" -> "Repair"
            "replacement" -> "Replacement"
            else -> raw
        }

    private fun formatServiceTime(date: LocalDate?): String {
        if (date == null) return ""
        return "${date.monthValue}.${date.dayOfMonth}"
    }

    private fun formatSerialNumbers(serial: String?, extra: String?): String {
        val parts = listOfNotNull(
            serial?.trim()?.takeIf { it.isNotEmpty() },
            extra?.trim()?.takeIf { it.isNotEmpty() },
        )
        return parts.joinToString("; ")
    }

    private fun formatOrderId(raw: String?): String {
        val value = raw?.trim()
        return if (value.isNullOrEmpty()) "none" else value
    }

    private fun normalizePurchaseChannel(raw: String?): String {
        val value = raw?.trim()?.lowercase() ?: return ""
        return when {
            "official" in value || "офици" in value -> "Official Store"
            "amazon" in value -> "Amazon"
            "reseller" in value || "ресел" in value -> "Reseller"
            else -> raw?.trim() ?: ""
        }
    }

    private fun normalizeWarrantyStatus(raw: String?): String {
        val value = raw?.trim()?.lowercase() ?: return ""
        return when {
            value.contains("non") || value.contains("не гарант") -> "Non-Warranty"
            value.contains("warranty") || value.contains("гарант") -> "Warranty"
            else -> raw?.trim() ?: ""
        }
    }

    private fun resolveProductType(productType: String?, printerModel: String?): String {
        productType?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        printerModel?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return ""
    }
}
