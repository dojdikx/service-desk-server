package com.servicedesk.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TicketReadStateTable : Table("ticket_read_state") {
    val staffId = integer("staff_id").references(SupportStaffTable.id)
    val ticketId = integer("ticket_id").references(TicketsTable.id)
    val lastReadAt = timestamp("last_read_at")

    override val primaryKey = PrimaryKey(staffId, ticketId)
}
