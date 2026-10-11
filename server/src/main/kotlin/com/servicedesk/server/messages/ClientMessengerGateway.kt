package com.servicedesk.server.messages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

interface ClientMessengerGateway {
    fun onStaffMessageSent(ticketId: Int, messageId: Int, text: String)
}

class DefaultClientMessengerGateway(
    private val messageRepository: MessageRepository,
    private val scope: CoroutineScope,
) : ClientMessengerGateway {

    override fun onStaffMessageSent(ticketId: Int, messageId: Int, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            delay(300 + Random.nextLong(0, 400))
            messageRepository.setDeliveryStatus(messageId, "delivered")
            if (Random.nextFloat() < 0.08f) return@launch
            delay(1200 + Random.nextLong(0, 2800))
            val reply = ClientReplyText.pick(trimmed)
            messageRepository.insertUserMessage(
                ticketId = ticketId,
                text = reply,
                replyToId = if (Random.nextBoolean()) messageId else null,
            )
        }
    }
}
