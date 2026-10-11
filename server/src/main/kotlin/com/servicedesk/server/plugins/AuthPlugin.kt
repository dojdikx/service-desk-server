package com.servicedesk.server.plugins

import com.servicedesk.server.auth.GetCurrentStaffUseCase
import com.servicedesk.server.auth.JwtService
import com.servicedesk.server.auth.LoginStaffUseCase
import com.servicedesk.server.auth.RegisterStaffUseCase
import com.servicedesk.server.auth.configureAuthRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

fun Application.configureAuth(
    registerStaffUseCase: RegisterStaffUseCase,
    loginStaffUseCase: LoginStaffUseCase,
    getCurrentStaffUseCase: GetCurrentStaffUseCase,
) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "service-desk"
            verifier(JwtService.verifier())
            validate { credential ->
                val staffId = JwtService.staffId(credential.payload) ?: return@validate null
                val role = credential.payload.getClaim("role").asString() ?: "support_agent"
                StaffPrincipal(staffId, role)
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "unauthorized"),
                )
            }
        }
    }
    configureAuthRoutes(registerStaffUseCase, loginStaffUseCase, getCurrentStaffUseCase)
}

data class StaffPrincipal(
    val staffId: Int,
    val role: String,
) : io.ktor.server.auth.Principal
