package com.servicedesk.server.staff

import com.servicedesk.server.plugins.StaffPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureStaffDirectoryRoutes(
    repository: StaffDirectoryRepository,
) {
    routing {
        authenticate("auth-jwt") {
            route("/api/staff") {
                get {
                    val principal = call.principal<StaffPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    if (principal.role != "admin") {
                        return@get call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
                    }
                    call.respond(StaffListResponse(staff = repository.listActive()))
                }
            }
        }
    }
}
