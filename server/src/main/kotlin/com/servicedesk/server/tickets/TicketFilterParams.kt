package com.servicedesk.server.tickets

import java.time.LocalDate

data class TicketFilterParams(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val tagStr: String? = null,
    val serialNumber: String? = null,
    val manager: Int? = null,
)

class FiltersRequiredException(
    message: String = "Укажите хотя бы один параметр фильтрации",
) : Exception(message)

class InvalidDateRangeException(
    message: String = "Дата начала не может быть позже даты окончания",
) : Exception(message)
