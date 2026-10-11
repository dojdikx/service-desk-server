package com.servicedesk.server.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import com.servicedesk.server.plugins.StaffPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Route.authRoutes(
    registerStaffUseCase: RegisterStaffUseCase,
    loginStaffUseCase: LoginStaffUseCase,
    getCurrentStaffUseCase: GetCurrentStaffUseCase,
) {
    authenticate("auth-jwt") {
        post("/register") {
            val principal = call.principal<StaffPrincipal>()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            if (principal.role != "admin") {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("code" to "FORBIDDEN"))
            }
            try {
                val request = call.receive<RegisterRequest>()
                call.respond(HttpStatusCode.Created, registerStaffUseCase.executeAsAdmin(request))
            } catch (e: EmailAlreadyExistsException) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "email_already_exists", "message" to "Этот email уже занят"),
                )
            } catch (e: ValidationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "validation_error", "message" to (e.message ?: "")),
                )
            }
        }
    }

    post("/login") {
        try {
            val request = call.receive<LoginRequest>()
            call.respond(HttpStatusCode.OK, loginStaffUseCase.execute(request))
        } catch (e: InvalidCredentialsException) {
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "invalid_credentials", "message" to "Неверная почта или пароль"),
            )
        } catch (e: InactiveAccountException) {
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "inactive_account", "message" to "Аккаунт отключён"),
            )
        }
    }

    post("/logout") {
        call.respond(HttpStatusCode.OK, MessageResponse("Выход выполнен"))
    }

    authenticate("auth-jwt") {
        get("/me") {
            val staffId = call.principal<StaffPrincipal>()?.staffId
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            try {
                call.respond(getCurrentStaffUseCase.execute(staffId))
            } catch (e: StaffNotFoundException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "not_found"))
            } catch (e: InactiveAccountException) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "inactive_account", "message" to "Аккаунт отключён"),
                )
            }
        }
    }
}

fun Application.configureAuthRoutes(
    registerStaffUseCase: RegisterStaffUseCase,
    loginStaffUseCase: LoginStaffUseCase,
    getCurrentStaffUseCase: GetCurrentStaffUseCase,
) {
    routing {
        route("/api/auth") {
            authRoutes(registerStaffUseCase, loginStaffUseCase, getCurrentStaffUseCase)
        }
    }
}
