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
    private val realtimeNotifier: com.servicedesk.server.realtime.RealtimeNotifier,
    private val scope: CoroutineScope,
) : ClientMessengerGateway {

    override fun onStaffMessageSent(payload: StaffOutboundPayload) {
        val trimmed = payload.text.trim()
        if (trimmed.isEmpty() && !payload.media.hasMedia()) return
        scope.launch {
            delay(300 + Random.nextLong(0, 400))
            val deliveryFailure = deliveryFailureReason(payload)
            if (deliveryFailure != null) {
                messageRepository.setDeliveryStatus(payload.messageId, "failed", deliveryFailure)
                realtimeNotifier.onMessageDeliveryUpdated(
                    payload.ticketId,
                    payload.messageId,
                    "failed",
                    deliveryFailure,
                )
                return@launch
            }
            messageRepository.setDeliveryStatus(payload.messageId, "delivered", null)
            realtimeNotifier.onMessageDeliveryUpdated(payload.ticketId, payload.messageId, "delivered")
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
            val created = messageRepository.insertUserMessage(
                ticketId = payload.ticketId,
                text = replyText,
                replyToId = if (Random.nextBoolean()) payload.messageId else null,
                media = mediaReply,
            )
            if (created != null) {
                realtimeNotifier.onMessageCreated(payload.ticketId, created)
            }
        }
    }

    private fun deliveryFailureReason(payload: StaffOutboundPayload): String? {
        if (payload.media.mediaType == "video") {
            return "Не удалось доставить видео в канал клиента."
        }
        if (payload.media.hasMedia() && Random.nextFloat() < 0.12f) {
            return "Вложение не доставлено. Клиенту отправлено пояснение."
        }
        if (Random.nextFloat() < 0.04f) {
            return "Временная ошибка канала связи."
        }
        return null
    }
}
