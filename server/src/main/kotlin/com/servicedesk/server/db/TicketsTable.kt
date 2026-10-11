package com.servicedesk.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object TicketsTable : Table("tickets") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UsersTable.id)
    val maxUserId = long("max_user_id").nullable()
    val status = varchar("status", 50)
    val tags = text("tags")
    val assignedTo = integer("assigned_to").references(SupportStaffTable.id).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val reason = text("reason").nullable()
    val printerModel = varchar("printer_model", 255).nullable()
    val serialNumber = varchar("serial_number", 255).nullable()
    val trackingNumber = varchar("tracking_number", 255).nullable()
    val deliveryAddress = text("delivery_address").nullable()
    val warrantyCase = bool("warranty_case").nullable()
    val purchasedFrom = varchar("purchased_from", 255).nullable()
    val sku = varchar("sku", 255).nullable()
    val deliveryCost = decimal("delivery_cost", 10, 2).nullable()
    val solutionDescription = text("solution_description").nullable()
    val fullName = varchar("full_name", 255).nullable()
    val contactPhone = varchar("contact_phone", 32).nullable()
    val externalExportUrl = text("external_export_url").nullable()
    val serviceType = varchar("service_type", 50).nullable()
    val serviceTime = date("service_time").nullable()
    val replenishmentRequirement = varchar("replenishment_requirement", 10).nullable()
    val serviceChannel = text("service_channel")
    val serviceSolution = varchar("service_solution", 100).nullable()
    val warrantyStatus = varchar("warranty_status", 50).nullable()
    val issueType = varchar("issue_type", 255).nullable()
    val subIssueType = varchar("sub_issue_type", 255).nullable()
    val thirdIssueType = varchar("third_issue_type", 255).nullable()
    val qtyUsed = integer("qty_used").nullable()
    val replacementQty = integer("replacement_qty").nullable()
    val replenishmentQty = integer("replenishment_qty").nullable()
    val skuRemark = text("sku_remark").nullable()
    val extraSerialNumber = varchar("extra_serial_number", 255).nullable()
    val purchaseOrderId = varchar("purchase_order_id", 255).nullable()
    val productType = varchar("product_type", 100).nullable()
    val replacementParts = text("replacement_parts")

    override val primaryKey = PrimaryKey(id)
}
