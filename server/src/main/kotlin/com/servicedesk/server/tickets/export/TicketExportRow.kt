package com.servicedesk.server.tickets.export

import com.servicedesk.server.tickets.ReplacementPartDto
import java.time.LocalDate

data class TicketExportRow(
    val id: Int,
    val serviceType: String,
    val serviceTime: LocalDate?,
    val replenishmentRequirement: String?,
    val serialNumber: String?,
    val extraSerialNumber: String?,
    val purchaseOrderId: String?,
    val purchasedFrom: String?,
    val serviceChannel: List<String>,
    val serviceSolution: String?,
    val productType: String?,
    val printerModel: String?,
    val warrantyStatus: String?,
    val reason: String?,
    val sku: String?,
    val issueType: String?,
    val subIssueType: String?,
    val thirdIssueType: String?,
    val qtyUsed: Int?,
    val replacementQty: Int?,
    val replenishmentQty: Int?,
    val skuRemark: String?,
    val replacementParts: List<ReplacementPartDto>,
)
