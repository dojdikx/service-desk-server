package com.servicedesk.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object MessagesTable : Table("messages") {
    val id = integer("id").autoIncrement()
    val ticketId = integer("ticket_id").references(TicketsTable.id)
    val messageText = text("message_text").nullable()
    val messageType = varchar("message_type", 20)
    val mediaType = varchar("media_type", 20).nullable()
    val mediaUrl = text("media_url").nullable()
    val mediaCaption = text("media_caption").nullable()
    val mediaFileName = text("media_file_name").nullable()
    val mediaItems = text("media_items").nullable()
    val timestamp = timestamp("timestamp")
    val deliveryStatus = varchar("delivery_status", 20)
    val deliveryError = text("delivery_error").nullable()
    val externalMessageId = text("external_message_id").nullable()
    val replyToId = integer("reply_to_id").nullable()
    val quoteText = text("quote_text").nullable()
    val quoteOffset = integer("quote_offset").nullable()

    override val primaryKey = PrimaryKey(id)
}
