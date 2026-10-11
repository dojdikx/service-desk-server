package com.servicedesk.server.tickets

data class StaffSession(
    val staffId: Int,
    val role: String,
) {
    val isAdmin: Boolean get() = role == "admin"
}
