package com.servicedesk.server.tickets

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TicketColumnConfigDto(
    val id: String,
    val status: String,
    val color: String,
    val isClosed: Boolean = false,
)

@Serializable
data class TicketColumnsFile(
    val defaultStatus: String,
    val columns: List<TicketColumnConfigDto>,
)

@Serializable
data class TicketColumnsResponse(
    val defaultStatus: String,
    val columns: List<TicketColumnConfigDto>,
)

object TicketColumnsConfig {
    private val json = Json { ignoreUnknownKeys = true }

    private val file: TicketColumnsFile by lazy {
        val text = TicketColumnsConfig::class.java.classLoader
            .getResourceAsStream("ticket/columns.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("ticket/columns.json not found")
        json.decodeFromString(TicketColumnsFile.serializer(), text)
    }

    fun columnsResponse(): TicketColumnsResponse =
        TicketColumnsResponse(defaultStatus = file.defaultStatus, columns = file.columns)

    fun defaultStatus(): String = file.defaultStatus

    fun validStatuses(): Set<String> = file.columns.map { it.status }.toSet()

    fun isValidStatus(status: String): Boolean = status in validStatuses()

    fun isClosedStatus(status: String): Boolean =
        file.columns.any { it.status == status && it.isClosed }
}
