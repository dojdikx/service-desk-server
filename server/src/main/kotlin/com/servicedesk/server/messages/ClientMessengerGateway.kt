package com.servicedesk.server.messages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

interface ClientMessengerGateway {
    fun onStaffMessageSent(payload: StaffOutboundPayload)
}

class DefaultClientMessengerGateway(
    private val messageRepository: MessageRepository,
    private val scope: CoroutineScope,
) : ClientMessengerGateway {

    override fun onStaffMessageSent(payload: StaffOutboundPayload) {
        val trimmed = payload.text.trim()
        if (trimmed.isEmpty() && !payload.media.hasMedia()) return
        scope.launch {
            delay(300 + Random.nextLong(0, 400))
            messageRepository.setDeliveryStatus(payload.messageId, "delivered")
            if (Random.nextFloat() < 0.08f) return@launch
            delay(1200 + Random.nextLong(0, 2800))
            val wantsPhoto = trimmed.contains("фото", ignoreCase = true) ||
                trimmed.contains("сним", ignoreCase = true)
            val mediaReply = when {
                wantsPhoto || payload.media.mediaType == "photo" -> MessageMediaFields(
                    mediaType = "photo",
                    mediaUrl = "/api/media/demo_client_printer.png",
                    mediaFileName = "printer.png",
                    mediaCaption = "Фото со стола",
                )
                else -> MessageMediaFields()
            }
            val replyText = when {
                mediaReply.hasMedia() && wantsPhoto -> "Вот фото, как просили."
                mediaReply.hasMedia() -> "Отправил фото."
                trimmed.isEmpty() && payload.media.hasMedia() ->
                    listOf("Получил, спасибо.", "Посмотрю и отвечу.").random()
                trimmed.isNotEmpty() -> ClientReplyText.pick(trimmed)
                else -> return@launch
            }
            messageRepository.insertUserMessage(
                ticketId = payload.ticketId,
                text = replyText,
                replyToId = if (Random.nextBoolean()) payload.messageId else null,
                media = mediaReply,
            )
        }
    }
}
