package com.servicedesk.server.staff

import kotlinx.serialization.Serializable

@Serializable
data class StaffBriefDto(
    val id: Int,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
)

@Serializable
data class StaffListResponse(
    val staff: List<StaffBriefDto>,
)
