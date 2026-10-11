package com.servicedesk.server.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PrinterSerialTest {
    @Test
    fun detectsModelByPrefix() {
        assertEquals("h2c", detectPrinterModelCode("31B1234567890123"))
    }
}
