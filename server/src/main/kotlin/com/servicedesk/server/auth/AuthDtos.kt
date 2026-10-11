package com.servicedesk.server.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
)

@Serializable
data class StaffPublicDto(
    val id: Int,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val role: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val staff: StaffPublicDto,
)

@Serializable
data class MeResponse(
    val staff: StaffPublicDto,
    val authenticated: Boolean = true,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class RegisterStaffResponse(
    val staff: StaffPublicDto,
)
