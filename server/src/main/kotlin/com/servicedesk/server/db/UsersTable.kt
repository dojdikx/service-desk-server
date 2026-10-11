package com.servicedesk.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val maxId = long("max_id").uniqueIndex()
    val maxChatId = long("max_chat_id").nullable()
    val name = varchar("name", 255)
    val phone = varchar("phone", 20).nullable()
    val city = varchar("city", 255)
    val printerModel = varchar("printer_model", 255).nullable()
    val serialNumber = varchar("serial_number", 255).nullable()
    val status = varchar("status", 50)
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val avatarSourceUrl = varchar("avatar_source_url", 512).nullable()
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(id)
}
