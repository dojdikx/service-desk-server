package com.servicedesk.server.media

import com.servicedesk.server.config.AppConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.notExists

class MediaStorage(
    private val root: Path = Path.of(AppConfig.mediaStoragePath),
) {
    init {
        if (root.notExists()) {
            Files.createDirectories(root)
        }
    }

    fun resolveKey(storageKey: String): Path? {
        if (!STORAGE_KEY_REGEX.matches(storageKey)) return null
        val resolved = root.resolve(storageKey).normalize()
        if (!resolved.startsWith(root)) return null
        return if (resolved.exists()) resolved else null
    }

    fun save(bytes: ByteArray, originalFileName: String, contentType: String): StoredMedia {
        val ext = extensionFrom(originalFileName, contentType)
        val key = "${UUID.randomUUID()}$ext"
        val target = root.resolve(key)
        Files.write(target, bytes)
        return StoredMedia(
            storageKey = key,
            mediaUrl = "/api/media/$key",
            mediaType = mediaTypeFrom(contentType, ext),
            fileName = sanitizeFileName(originalFileName),
            contentType = contentType,
            sizeBytes = bytes.size.toLong(),
        )
    }

    fun copyClasspathAsset(resourcePath: String, storageKey: String) {
        if (!STORAGE_KEY_REGEX.matches(storageKey)) return
        val target = root.resolve(storageKey)
        if (target.exists()) return
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath) ?: return
        stream.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun open(storageKey: String): Pair<Path, String>? {
        val path = resolveKey(storageKey) ?: return null
        val contentType = contentTypeFromPath(path)
        return path to contentType
    }

    private fun sanitizeFileName(name: String): String =
        name.substringAfterLast('\\').substringAfterLast('/').take(200).ifBlank { "file" }

    private fun extensionFrom(fileName: String, contentType: String): String {
        val fromName = fileName.substringAfterLast('.', "").lowercase().takeIf { it.length in 2..5 }
        if (fromName != null) return ".$fromName"
        return when {
            contentType.startsWith("image/png") -> ".png"
            contentType.startsWith("image/jpeg") -> ".jpg"
            contentType.startsWith("image/webp") -> ".webp"
            contentType.startsWith("video/") -> ".mp4"
            contentType == "application/pdf" -> ".pdf"
            else -> ".bin"
        }
    }

    private fun mediaTypeFrom(contentType: String, ext: String): String = when {
        contentType.startsWith("image/") -> "photo"
        contentType.startsWith("video/") -> "video"
        contentType == "application/pdf" || ext == ".pdf" -> "document"
        else -> "document"
    }

    private fun contentTypeFromPath(path: Path): String {
        val name = path.fileName.toString().lowercase()
        return when {
            name.endsWith(".png") -> "image/png"
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".mp4") -> "video/mp4"
            name.endsWith(".pdf") -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    companion object {
        private val STORAGE_KEY_REGEX = Regex("^[a-zA-Z0-9._-]+$")

        fun ensureLayout(root: Path = Path.of(AppConfig.mediaStoragePath)) {
            val storage = MediaStorage(root)
            storage.copyClasspathAsset("media/demo_client_printer.png", "demo_client_printer.png")
        }
    }
}

data class StoredMedia(
    val storageKey: String,
    val mediaUrl: String,
    val mediaType: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
)
