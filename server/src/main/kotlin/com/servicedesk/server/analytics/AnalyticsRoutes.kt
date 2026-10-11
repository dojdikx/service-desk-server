package com.servicedesk.server.analytics

import com.servicedesk.server.plugins.StaffPrincipal
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.nio.file.Files

fun Application.configureAnalyticsRoutes(
    getAnalyticsUseCase: GetAnalyticsUseCase,
    storage: AnalyticsReportStorage,
) {
    routing {
        authenticate("auth-jwt") {
            route("/api/analytics") {
                post {
                    if (call.principal<StaffPrincipal>() == null) {
                        return@post call.respond(HttpStatusCode.Unauthorized)
                    }
                    try {
                        val body = call.receive<AnalyticsRequest>()
                        when (val result = getAnalyticsUseCase.execute(body)) {
                            is AnalyticsUseCaseResult.Excel -> call.respond(result.body)
                            is AnalyticsUseCaseResult.Report -> call.respond(result.body)
                        }
                    } catch (_: InvalidAnalyticsDateRangeException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("code" to "INVALID_DATE_RANGE"))
                    } catch (_: AnalyticsChannelsRequiredException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("code" to "CHANNELS_REQUIRED"))
                    }
                }
                get("/files/{fileName}") {
                    if (call.principal<StaffPrincipal>() == null) {
                        return@get call.respond(HttpStatusCode.Unauthorized)
                    }
                    val fileName = call.parameters["fileName"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val path = storage.resolveFileName(fileName)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    val bytes = Files.readAllBytes(path)
                    call.respondBytes(bytes, ContentType.Application.OctetStream)
                }
            }
        }
    }
}
