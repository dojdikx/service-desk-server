package com.servicedesk.server.tickets.export

import com.servicedesk.server.analytics.AnalyticsReportStorage
import java.time.LocalDate

class ExportTicketsUseCase(
    private val repository: TicketsExportRepository,
    private val storage: AnalyticsReportStorage,
) {
    suspend fun execute(request: TicketsExportRequest): TicketsExportResponse {
        val start = LocalDate.parse(request.startDate)
        val end = LocalDate.parse(request.endDate)
        if (start > end) throw InvalidTicketsExportDateRangeException()
        val channels = normalizeChannels(request.channels)
        if (channels.isEmpty()) throw TicketsExportChannelsRequiredException()

        val tickets = repository.findForExport(start, end, channels)
        val channelsSuffix = channels.joinToString("_")
        val fileName = "tickets_export_${channelsSuffix}_с_${start}_по_${end}.xlsx"
        val path = storage.targetPath(fileName)
        TicketsExportExcelGenerator.write(tickets, path)
        return TicketsExportResponse(excelPath = "/api/analytics/files/$fileName")
    }

    private fun normalizeChannels(channels: List<String>): List<String> {
        val allowed = channels.map { it.lowercase() }.filter { it == "max" }.distinct()
        return allowed.ifEmpty { listOf("max") }
    }
}
