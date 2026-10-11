package com.servicedesk.server.messages

data class MessageMediaFields(
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val mediaCaption: String? = null,
    val mediaFileName: String? = null,
    val mediaItems: List<MediaItemDto>? = null,
) {
    fun hasMedia(): Boolean =
        !mediaUrl.isNullOrBlank() || !mediaItems.isNullOrEmpty()

    companion object {
        fun fromRequest(request: SendMessageRequest): MessageMediaFields =
            MessageMediaFields(
                mediaType = request.mediaType,
                mediaUrl = request.mediaUrl,
                mediaCaption = request.mediaCaption,
                mediaFileName = request.mediaFileName,
                mediaItems = request.mediaItems,
            )
    }
}
