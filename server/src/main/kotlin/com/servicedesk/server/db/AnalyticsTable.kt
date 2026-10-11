package com.servicedesk.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object AnalyticsTable : Table("analytics") {
    val id = integer("id").autoIncrement()
    val path = text("path")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
