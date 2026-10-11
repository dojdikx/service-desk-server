package com.servicedesk.server.tickets

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val ticketJson = Json { ignoreUnknownKeys = true }

fun parseStringListJson(raw: String?): List<String> {
    if (raw.isNullOrBlank() || raw == "null") return emptyList()
    return runCatching {
        ticketJson.decodeFromString(ListSerializer(String.serializer()), raw)
    }.getOrElse { emptyList() }
}

fun stringListToJson(values: List<String>?): String =
    ticketJson.encodeToString(ListSerializer(String.serializer()), values ?: emptyList())
