package com.servicedesk.server.messages

import kotlin.random.Random

internal object ClientReplyText {
    private val defaults = listOf(
        "Спасибо, понял!",
        "Хорошо, жду.",
        "Ок, спасибо за ответ.",
        "Принял, сейчас проверю.",
    )

    fun pick(staffText: String): String {
        val lower = staffText.lowercase()
        return when {
            "фото" in lower || "сним" in lower || "видео" in lower ->
                listOf(
                    "Сейчас пришлю фото.",
                    "Минуту, сделаю снимок.",
                    "Ок, отправлю видео чуть позже.",
                ).random()
            "серийн" in lower || "номер" in lower ->
                listOf(
                    "Серийный номер на наклейке сзади, сейчас напишу.",
                    "СН: ${fakeSerial()}",
                ).random()
            "адрес" in lower || "достав" in lower ->
                listOf(
                    "Адрес тот же, что в заявке.",
                    "Можно отправить по адресу из профиля.",
                ).random()
            "запчаст" in lower || "sku" in lower ->
                listOf(
                    "Подскажите, когда ожидать запчасть?",
                    "SKU верный, жду отправку.",
                ).random()
            "?" in staffText ->
                listOf(
                    "Да, конечно.",
                    "Сейчас уточню и отвечу.",
                    "Пока не знаю, посмотрю сегодня вечером.",
                ).random()
            staffText.length < 4 ->
                listOf("Ок", "Хорошо", "Понял").random()
            else -> defaults.random()
        }
    }

    private fun fakeSerial(): String {
        val digits = (1..13).map { Random.nextInt(0, 10) }.joinToString("")
        return digits
    }
}
