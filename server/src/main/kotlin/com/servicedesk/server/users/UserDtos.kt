package com.servicedesk.server.users

import kotlinx.serialization.Serializable

@Serializable
data class UserShortDto(
    val id: Int,
    val name: String,
    val phone: String,
    val city: String,
)

@Serializable
data class UserListResponse(
    val users: List<UserShortDto>,
)
