package com.servicedesk.server.tickets.export

import kotlinx.serialization.Serializable

@Serializable
data class TicketsExportRequest(
    val startDate: String,
    val endDate: String,
    val channels: List<String> = listOf("max"),
)

@Serializable
data class TicketsExportResponse(
    val excelPath: String,
)
