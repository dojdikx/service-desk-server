package com.servicedesk.server.analytics

import com.servicedesk.server.tickets.TicketColumnsConfig
import java.time.LocalDate

sealed class AnalyticsUseCaseResult {
    data class Report(val body: AnalyticsResponse) : AnalyticsUseCaseResult()
    data class Excel(val body: AnalyticsExcelResponse) : AnalyticsUseCaseResult()
}

class GetAnalyticsUseCase(
    private val repository: AnalyticsRepository,
    private val storage: AnalyticsReportStorage,
) {
    suspend fun execute(request: AnalyticsRequest): AnalyticsUseCaseResult {
        val start = LocalDate.parse(request.startDate)
        val end = LocalDate.parse(request.endDate)
        if (start > end) throw InvalidAnalyticsDateRangeException()
        val channels = normalizeChannels(request.channels)
        if (channels.isEmpty()) throw AnalyticsChannelsRequiredException()

        val query = AnalyticsQuery(
            startDate = start,
            endDate = end,
            channels = channels,
            includeStatusBreakdown = request.includeStatusBreakdown,
            includePrinterModelsBreakdown = request.includePrinterModelsBreakdown,
            includeTicketsBreakdown = request.includeTicketsBreakdown,
        )
        val data = repository.compute(query)

        if (request.excel) {
            val kind = if (request.includeStatusBreakdown) "detailed" else "brief"
            val fileName = "${kind}_analytics_${start}_${end}.xlsx"
            val path = storage.targetPath(fileName)
            AnalyticsExcelGenerator.write(data, path)
            val webPath = "/api/analytics/files/$fileName"
            repository.saveReportPath(webPath)
            return AnalyticsUseCaseResult.Excel(AnalyticsExcelResponse(excelPath = webPath))
        }

        return AnalyticsUseCaseResult.Report(
            AnalyticsResponse(
                period = data.period,
                ticketsCreated = data.ticketsCreated,
                ticketsModified = data.ticketsModified,
                statusBreakdown = if (request.includeStatusBreakdown) {
                    val counts = data.statusBreakdown
                    val columns = TicketColumnsConfig.columnsResponse().columns
                    val known = columns.map { it.status }.toSet()
                    columns.map { column ->
                        StatusCountDto(status = column.status, count = counts[column.status] ?: 0)
                    } + counts.filterKeys { it !in known }.map { StatusCountDto(it.key, it.value) }
                } else {
                    null
                },
                printerModelBreakdown = if (request.includePrinterModelsBreakdown) {
                    data.printerModelBreakdown.entries
                        .map { PrinterModelCountDto(it.key, it.value) }
                        .sortedByDescending { it.count }
                } else {
                    null
                },
                ticketsBreakdown = if (request.includeTicketsBreakdown) {
                    data.ticketsBreakdown
                } else {
                    null
                },
            ),
        )
    }

    private fun normalizeChannels(channels: List<String>): List<String> {
        val allowed = channels.map { it.lowercase() }.filter { it == "max" }.distinct()
        return allowed.ifEmpty { listOf("max") }
    }
}
