package com.servicedesk.server.media

import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    val storageKey: String,
    val mediaUrl: String,
    val mediaType: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
)
