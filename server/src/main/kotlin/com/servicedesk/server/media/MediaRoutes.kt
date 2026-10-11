package com.servicedesk.server.media

import com.servicedesk.server.plugins.StaffPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val MAX_UPLOAD_BYTES = 15 * 1024 * 1024

fun Application.configureMediaRoutes(storage: MediaStorage) {
    routing {
        authenticate("auth-jwt") {
            route("/api/media") {
                post("/upload") {
                    if (call.principal<StaffPrincipal>() == null) {
                        return@post call.respond(HttpStatusCode.Unauthorized)
                    }
                    val multipart = call.receiveMultipart()
                    var stored: StoredMedia? = null
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                if (stored == null) {
                                    val bytes = part.streamProvider().use { it.readBytes() }
                                    if (bytes.isNotEmpty() && bytes.size <= MAX_UPLOAD_BYTES) {
                                        val fileName = part.originalFileName ?: "upload"
                                        val contentType = part.contentType?.toString() ?: "application/octet-stream"
                                        stored = storage.save(bytes, fileName, contentType)
                                    }
                                }
                            }
                            else -> Unit
                        }
                        part.dispose()
                    }
                    val result = stored
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("code" to "FILE_REQUIRED"))
                    call.respond(
                        MediaUploadResponse(
                            storageKey = result.storageKey,
                            mediaUrl = result.mediaUrl,
                            mediaType = result.mediaType,
                            fileName = result.fileName,
                            contentType = result.contentType,
                            sizeBytes = result.sizeBytes,
                        ),
                    )
                }
                get("/{storageKey}") {
                    if (call.principal<StaffPrincipal>() == null) {
                        return@get call.respond(HttpStatusCode.Unauthorized)
                    }
                    val key = call.parameters["storageKey"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val (path, contentType) = storage.open(key)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    val bytes = java.nio.file.Files.readAllBytes(path)
                    call.respondBytes(bytes, io.ktor.http.ContentType.parse(contentType))
                }
            }
        }
    }
}
