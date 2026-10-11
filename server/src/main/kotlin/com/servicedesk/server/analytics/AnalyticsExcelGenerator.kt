package com.servicedesk.server.analytics

import java.nio.file.Path
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object AnalyticsExcelGenerator {
    fun write(data: AnalyticsData, target: Path) {
        XSSFWorkbook().use { workbook ->
            val summary = workbook.createSheet("Сводка")
            var rowIdx = 0
            summary.createRow(rowIdx++).apply {
                createCell(0).setCellValue("Период")
                createCell(1).setCellValue("${data.period.startDate} — ${data.period.endDate}")
            }
            summary.createRow(rowIdx++).apply {
                createCell(0).setCellValue("Каналы")
                createCell(1).setCellValue(data.period.channels.joinToString(", "))
            }
            summary.createRow(rowIdx++).apply {
                createCell(0).setCellValue("Создано заявок")
                createCell(1).setCellValue(data.ticketsCreated.toDouble())
            }
            summary.createRow(rowIdx++).apply {
                createCell(0).setCellValue("Изменено заявок")
                createCell(1).setCellValue(data.ticketsModified.toDouble())
            }

            if (data.statusBreakdown.isNotEmpty()) {
                val sheet = workbook.createSheet("Статусы")
                sheet.createRow(0).apply {
                    createCell(0).setCellValue("Статус")
                    createCell(1).setCellValue("Количество")
                }
                data.statusBreakdown.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
                    sheet.createRow(index + 1).apply {
                        createCell(0).setCellValue(entry.key)
                        createCell(1).setCellValue(entry.value.toDouble())
                    }
                }
            }

            if (data.printerModelBreakdown.isNotEmpty()) {
                val sheet = workbook.createSheet("Модели")
                sheet.createRow(0).apply {
                    createCell(0).setCellValue("Модель")
                    createCell(1).setCellValue("Количество")
                }
                data.printerModelBreakdown.entries.sortedByDescending { it.value }
                    .forEachIndexed { index, entry ->
                        sheet.createRow(index + 1).apply {
                            createCell(0).setCellValue(entry.key)
                            createCell(1).setCellValue(entry.value.toDouble())
                        }
                    }
            }

            if (data.ticketsBreakdown.isNotEmpty()) {
                val sheet = workbook.createSheet("Заявки")
                sheet.createRow(0).apply {
                    createCell(0).setCellValue("ID")
                    createCell(1).setCellValue("Статус")
                    createCell(2).setCellValue("Создана")
                    createCell(3).setCellValue("Модель")
                    createCell(4).setCellValue("Серийный номер")
                    createCell(5).setCellValue("Причина")
                }
                data.ticketsBreakdown.forEachIndexed { index, ticket ->
                    sheet.createRow(index + 1).apply {
                        createCell(0).setCellValue(ticket.id.toDouble())
                        createCell(1).setCellValue(ticket.status)
                        createCell(2).setCellValue(ticket.createdAt)
                        createCell(3).setCellValue(ticket.printerModel ?: "")
                        createCell(4).setCellValue(ticket.serialNumber ?: "")
                        createCell(5).setCellValue(ticket.reason ?: "")
                    }
                }
            }

            if (data.managerEfficiency.isNotEmpty()) {
                val sheet = workbook.createSheet("Менеджеры")
                sheet.createRow(0).apply {
                    createCell(0).setCellValue("Email")
                    createCell(1).setCellValue("Имя")
                    createCell(2).setCellValue("Всего")
                    createCell(3).setCellValue("Открытые")
                    createCell(4).setCellValue("Закрытые")
                }
                data.managerEfficiency.forEachIndexed { index, row ->
                    sheet.createRow(index + 1).apply {
                        createCell(0).setCellValue(row.email)
                        createCell(1).setCellValue(
                            listOfNotNull(row.firstName, row.lastName).joinToString(" ").trim(),
                        )
                        createCell(2).setCellValue(row.totalTickets.toDouble())
                        createCell(3).setCellValue(row.openTickets.toDouble())
                        createCell(4).setCellValue(row.closedTickets.toDouble())
                    }
                }
            }

            target.toFile().outputStream().use { out ->
                workbook.write(out)
            }
        }
    }
}
