package com.servicedesk.server.util

private val prefixToModel = mapOf(
    "31B" to "h2c",
    "094" to "h2d",
    "239" to "h2d_pro",
    "093" to "h2s",
    "00M" to "x1c",
    "03W" to "x1e",
    "20P" to "x2d",
    "01P" to "p1s",
    "01S" to "p1p",
    "22E" to "p2s",
    "039" to "a1",
    "030" to "a1_mini",
    "26A" to "a2l",
)

private val modelLabels = mapOf(
    "h2c" to "H2C",
    "h2d" to "H2D",
    "h2d_pro" to "H2D Pro",
    "h2s" to "H2S",
    "x1c" to "X1C",
    "x1e" to "X1E",
    "x2d" to "X2D",
    "p1s" to "P1S",
    "p1p" to "P1P",
    "p2s" to "P2S",
    "a1" to "A1",
    "a1_mini" to "A1 mini",
    "a2l" to "A2L",
)

fun normalizeSerialNumber(serialNumber: String): String =
    serialNumber.trim().replace("\\s+".toRegex(), "").uppercase()

fun normalizeSerialSearch(serialNumber: String): String {
    val normalized = normalizeSerialNumber(serialNumber)
    return if (normalized.startsWith("SN")) normalized.drop(2) else normalized
}

fun detectPrinterModelCode(serialNumber: String?): String? {
    if (serialNumber.isNullOrBlank()) return null
    val normalized = normalizeSerialNumber(serialNumber)
    if (normalized.length < 3) return null
    return prefixToModel[normalized.take(3)]
}

fun detectPrinterModelLabel(serialNumber: String?): String? =
    detectPrinterModelCode(serialNumber)?.let { modelLabels[it] ?: it }
