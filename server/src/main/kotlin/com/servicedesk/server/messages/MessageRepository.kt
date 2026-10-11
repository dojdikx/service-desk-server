package com.servicedesk.server.messages

import com.servicedesk.server.db.MessagesTable
import com.servicedesk.server.db.TicketReadStateTable
import com.servicedesk.server.db.TicketsTable
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

interface MessageRepository {
    suspend fun listPreview(ticketId: Int, limit: Int = 30): List<MessageDto>
    suspend fun listBefore(ticketId: Int, beforeId: Int, limit: Int = 30): List<MessageDto>
    suspend fun sendBackendMessage(
        ticketId: Int,
        text: String,
        replyToId: Int? = null,
        quoteText: String? = null,
        quoteOffset: Int? = null,
        media: MessageMediaFields = MessageMediaFields(),
    ): MessageDto?
    suspend fun markTicketRead(staffId: Int, ticketId: Int)
    suspend fun unreadCount(staffId: Int, ticketId: Int): Int
    suspend fun ticketsWithMessages(): List<Int>
    suspend fun lastMessageText(ticketId: Int): String?
    suspend fun setDeliveryStatus(messageId: Int, status: String, deliveryError: String? = null)
    suspend fun findById(messageId: Int): MessageDto?

    suspend fun insertUserMessage(
        ticketId: Int,
        text: String,
        replyToId: Int? = null,
        media: MessageMediaFields = MessageMediaFields(),
    ): MessageDto?
}

class ExposedMessageRepository : MessageRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listPreview(ticketId: Int, limit: Int): List<MessageDto> =
        newSuspendedTransaction(Dispatchers.IO) {
            MessagesTable
                .selectAll()
                .where { MessagesTable.ticketId eq ticketId }
                .orderBy(MessagesTable.id to SortOrder.DESC)
                .limit(limit)
                .map { it.toDto() }
                .reversed()
        }

    override suspend fun listBefore(ticketId: Int, beforeId: Int, limit: Int): List<MessageDto> =
        newSuspendedTransaction(Dispatchers.IO) {
            MessagesTable
                .selectAll()
                .where { (MessagesTable.ticketId eq ticketId) and (MessagesTable.id less beforeId) }
                .orderBy(MessagesTable.id to SortOrder.DESC)
                .limit(limit)
                .map { it.toDto() }
                .reversed()
        }

    override suspend fun sendBackendMessage(
        ticketId: Int,
        text: String,
        replyToId: Int?,
        quoteText: String?,
        quoteOffset: Int?,
        media: MessageMediaFields,
    ): MessageDto? =
        newSuspendedTransaction(Dispatchers.IO) {
            val exists = TicketsTable.selectAll().where { TicketsTable.id eq ticketId }.any()
            if (!exists) return@newSuspendedTransaction null
            val now = Instant.now()
            val id = MessagesTable.insert {
                it[MessagesTable.ticketId] = ticketId
                it[messageText] = text.takeIf { it.isNotBlank() }
                it[messageType] = "backend"
                it[timestamp] = now
                it[deliveryStatus] = "pending"
                it[MessagesTable.replyToId] = replyToId
                it[MessagesTable.quoteText] = quoteText
                it[MessagesTable.quoteOffset] = quoteOffset
                it[mediaType] = media.mediaType
                it[mediaUrl] = media.mediaUrl
                it[mediaCaption] = media.mediaCaption
                it[mediaFileName] = media.mediaFileName
                it[MessagesTable.mediaItems] = encodeMediaItems(media.mediaItems)
            }[MessagesTable.id]
            MessagesTable.selectAll().where { MessagesTable.id eq id }.first().toDto()
        }

    override suspend fun markTicketRead(staffId: Int, ticketId: Int) {
        newSuspendedTransaction(Dispatchers.IO) {
            val now = Instant.now()
            val updated = TicketReadStateTable.update({
                (TicketReadStateTable.staffId eq staffId) and (TicketReadStateTable.ticketId eq ticketId)
            }) {
                it[lastReadAt] = now
            }
            if (updated == 0) {
                TicketReadStateTable.insert {
                    it[TicketReadStateTable.staffId] = staffId
                    it[TicketReadStateTable.ticketId] = ticketId
                    it[lastReadAt] = now
                }
            }
        }
    }

    override suspend fun unreadCount(staffId: Int, ticketId: Int): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            val lastRead = TicketReadStateTable
                .selectAll()
                .where {
                    (TicketReadStateTable.staffId eq staffId) and (TicketReadStateTable.ticketId eq ticketId)
                }
                .firstOrNull()
                ?.get(TicketReadStateTable.lastReadAt)
                ?: Instant.EPOCH

            MessagesTable
                .selectAll()
                .where {
                    (MessagesTable.ticketId eq ticketId) and
                        (MessagesTable.messageType eq "user") and
                        (MessagesTable.timestamp greater lastRead)
                }
                .count()
                .toInt()
        }

    override suspend fun ticketsWithMessages(): List<Int> =
        newSuspendedTransaction(Dispatchers.IO) {
            MessagesTable
                .select(MessagesTable.ticketId)
                .withDistinct()
                .map { it[MessagesTable.ticketId] }
        }

    override suspend fun lastMessageText(ticketId: Int): String? =
        newSuspendedTransaction(Dispatchers.IO) {
            MessagesTable
                .selectAll()
                .where { MessagesTable.ticketId eq ticketId }
                .orderBy(MessagesTable.id to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { row ->
                    row[MessagesTable.messageText]?.trim()?.takeIf { it.isNotEmpty() }
                        ?: mediaPreviewLabel(row[MessagesTable.mediaType], row[MessagesTable.mediaFileName])
                }
        }

    override suspend fun findById(messageId: Int): MessageDto? =
        newSuspendedTransaction(Dispatchers.IO) {
            MessagesTable
                .selectAll()
                .where { MessagesTable.id eq messageId }
                .firstOrNull()
                ?.toDto()
        }

    override suspend fun setDeliveryStatus(messageId: Int, status: String, deliveryError: String?) {
        newSuspendedTransaction(Dispatchers.IO) {
            MessagesTable.update({ MessagesTable.id eq messageId }) {
                it[MessagesTable.deliveryStatus] = status
                it[MessagesTable.deliveryError] = deliveryError
            }
        }
    }

    override suspend fun insertUserMessage(
        ticketId: Int,
        text: String,
        replyToId: Int?,
        media: MessageMediaFields,
    ): MessageDto? =
        newSuspendedTransaction(Dispatchers.IO) {
            val exists = TicketsTable.selectAll().where { TicketsTable.id eq ticketId }.any()
            if (!exists) return@newSuspendedTransaction null
            val now = Instant.now()
            val id = MessagesTable.insert {
                it[MessagesTable.ticketId] = ticketId
                it[messageText] = text.takeIf { it.isNotBlank() }
                it[messageType] = "user"
                it[timestamp] = now
                it[deliveryStatus] = "delivered"
                it[MessagesTable.replyToId] = replyToId
                it[mediaType] = media.mediaType
                it[mediaUrl] = media.mediaUrl
                it[mediaCaption] = media.mediaCaption
                it[mediaFileName] = media.mediaFileName
                it[MessagesTable.mediaItems] = encodeMediaItems(media.mediaItems)
            }[MessagesTable.id]
            MessagesTable.selectAll().where { MessagesTable.id eq id }.first().toDto()
        }

    private fun encodeMediaItems(items: List<MediaItemDto>?): String? =
        items?.takeIf { it.isNotEmpty() }?.let {
            json.encodeToString(ListSerializer(MediaItemDto.serializer()), it)
        }

    private fun decodeMediaItems(raw: String?): List<MediaItemDto>? =
        raw?.takeIf { it.isNotBlank() }?.let {
            json.decodeFromString(ListSerializer(MediaItemDto.serializer()), it)
        }

    private fun mediaPreviewLabel(mediaType: String?, fileName: String?): String? = when (mediaType) {
        "photo" -> "Фото"
        "video" -> "Видео"
        "document" -> fileName ?: "Документ"
        "album" -> "Альбом"
        else -> null
    }

    private fun ResultRow.toDto(): MessageDto {
        val ts = this[MessagesTable.timestamp]
        return MessageDto(
            id = this[MessagesTable.id],
            ticketId = this[MessagesTable.ticketId],
            messageText = this[MessagesTable.messageText] ?: "",
            messageType = this[MessagesTable.messageType],
            timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ts.atOffset(ZoneOffset.UTC)),
            mediaType = this[MessagesTable.mediaType],
            mediaUrl = this[MessagesTable.mediaUrl],
            mediaCaption = this[MessagesTable.mediaCaption],
            mediaFileName = this[MessagesTable.mediaFileName],
            mediaItems = decodeMediaItems(this[MessagesTable.mediaItems]),
            deliveryStatus = this[MessagesTable.deliveryStatus],
            deliveryError = this[MessagesTable.deliveryError],
            replyToId = this[MessagesTable.replyToId],
            quoteText = this[MessagesTable.quoteText],
            quoteOffset = this[MessagesTable.quoteOffset],
        )
    }
}
