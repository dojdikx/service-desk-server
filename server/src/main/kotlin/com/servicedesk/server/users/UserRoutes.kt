package com.servicedesk.server.users

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

fun Application.configureUserRoutes(
    userRepository: UserRepository,
) {
    routing {
        authenticate("auth-jwt") {
            route("/api/users") {
                get {
                    if (call.principal<StaffPrincipal>() == null) {
                        return@get call.respond(HttpStatusCode.Unauthorized)
                    }
                    call.respond(
                        UserListResponse(users = userRepository.listMaxUsers()),
                    )
                }
            }
        }
    }
}
