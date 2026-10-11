package com.servicedesk.server.tickets

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

@Serializable
data class TicketShortDto(
    val ticketId: Int,
    val userId: Int,
    val tags: List<String>? = null,
    val createdAt: String,
    val clientName: String? = null,
    val clientAvatarUrl: String? = null,
    val lastMessage: String? = null,
    val status: String,
    val assignedTo: Int? = null,
    val assigneeName: String? = null,
    val unreadCount: Int = 0,
    val serialNumber: String? = null,
)

@Serializable
data class TicketListResponse(
    val tickets: List<TicketShortDto>,
)

@Serializable
data class TicketCreateRequest(
    val userId: Int,
    val status: String? = null,
    val tags: List<String>? = null,
    val reason: String? = null,
    val printerModel: String? = null,
    val serialNumber: String? = null,
    val fullName: String? = null,
    val contactPhone: String? = null,
    val assignedTo: Int? = null,
)

@Serializable
data class ReplacementPartDto(
    val sku: String? = null,
    val qty: Int? = null,
    val remark: String? = null,
)

@Serializable
data class TicketPatchRequest(
    val status: String? = null,
    val tags: List<String>? = null,
    val reason: String? = null,
    val printerModel: String? = null,
    val serialNumber: String? = null,
    val trackingNumber: String? = null,
    val deliveryAddress: String? = null,
    val warrantyCase: Boolean? = null,
    val purchasedFrom: String? = null,
    val sku: String? = null,
    val deliveryCost: Double? = null,
    val solutionDescription: String? = null,
    val fullName: String? = null,
    val contactPhone: String? = null,
    val externalExportUrl: String? = null,
    val assignedTo: Int? = null,
    val serviceType: String? = null,
    val serviceTime: String? = null,
    val replenishmentRequirement: String? = null,
    val serviceChannel: List<String>? = null,
    val serviceSolution: String? = null,
    val warrantyStatus: String? = null,
    val issueType: String? = null,
    val subIssueType: String? = null,
    val thirdIssueType: String? = null,
    val qtyUsed: Int? = null,
    val replacementQty: Int? = null,
    val replenishmentQty: Int? = null,
    val skuRemark: String? = null,
    val extraSerialNumber: String? = null,
    val purchaseOrderId: String? = null,
    val productType: String? = null,
    val replacementParts: List<ReplacementPartDto>? = null,
)

@Serializable
data class AssignTicketRequest(
    val managerId: Int,
)

@Serializable
data class TicketDetailDto(
    val id: Int,
    val userId: Int,
    val status: String,
    val tags: List<String>? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val reason: String? = null,
    val printerModel: String? = null,
    val serialNumber: String? = null,
    val trackingNumber: String? = null,
    val deliveryAddress: String? = null,
    val warrantyCase: Boolean? = null,
    val purchasedFrom: String? = null,
    val sku: String? = null,
    val deliveryCost: Double? = null,
    val solutionDescription: String? = null,
    val fullName: String? = null,
    val contactPhone: String? = null,
    val externalExportUrl: String? = null,
    val assignedTo: Int? = null,
    val maxUserId: Long? = null,
    val serviceType: String? = null,
    val serviceTime: String? = null,
    val replenishmentRequirement: String? = null,
    val serviceChannel: List<String>? = null,
    val serviceSolution: String? = null,
    val warrantyStatus: String? = null,
    val issueType: String? = null,
    val subIssueType: String? = null,
    val thirdIssueType: String? = null,
    val qtyUsed: Int? = null,
    val replacementQty: Int? = null,
    val replenishmentQty: Int? = null,
    val skuRemark: String? = null,
    val extraSerialNumber: String? = null,
    val purchaseOrderId: String? = null,
    val productType: String? = null,
    val replacementParts: List<ReplacementPartDto>? = null,
)

private val tagsJson = Json { ignoreUnknownKeys = true }

fun parseTagsJson(raw: String?): List<String>? {
    if (raw.isNullOrBlank() || raw == "null") return emptyList()
    return runCatching {
        val element = tagsJson.parseToJsonElement(raw)
        element.jsonArray.mapNotNull { (it as? JsonPrimitive)?.content }
    }.getOrElse { emptyList() }
}

fun tagsToJson(tags: List<String>?): String =
    tagsJson.encodeToString(ListSerializer(String.serializer()), tags ?: emptyList())

fun parseReplacementPartsForApi(raw: String?): List<ReplacementPartDto>? {
    if (raw.isNullOrBlank() || raw == "null") return emptyList()
    return runCatching {
        tagsJson.decodeFromString(ListSerializer(ReplacementPartDto.serializer()), raw)
    }.getOrElse { emptyList() }
}

fun replacementPartsDtoToJson(parts: List<ReplacementPartDto>?): String =
    tagsJson.encodeToString(ListSerializer(ReplacementPartDto.serializer()), parts ?: emptyList())

fun localDateToApiString(date: LocalDate?): String? = date?.toString()
