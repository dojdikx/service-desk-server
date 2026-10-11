package com.servicedesk.server.auth

import com.servicedesk.server.db.SupportStaffTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant

data class StaffRecord(
    val id: Int,
    val email: String,
    val passwordHash: String,
    val firstName: String?,
    val lastName: String?,
    val role: String,
    val isActive: Boolean,
)

interface StaffAuthRepository {
    suspend fun findByEmail(email: String): StaffRecord?
    suspend fun findById(id: Int): StaffRecord?
    suspend fun create(
        email: String,
        passwordHash: String,
        firstName: String?,
        lastName: String?,
        role: String = "support_agent",
    ): StaffRecord
}

class ExposedStaffAuthRepository : StaffAuthRepository {

    override suspend fun findByEmail(email: String): StaffRecord? =
        newSuspendedTransaction(Dispatchers.IO) {
            SupportStaffTable.selectAll()
                .where { SupportStaffTable.email eq email.trim().lowercase() }
                .limit(1)
                .firstOrNull()
                ?.toStaffRecord()
        }

    override suspend fun findById(id: Int): StaffRecord? =
        newSuspendedTransaction(Dispatchers.IO) {
            SupportStaffTable.selectAll()
                .where { SupportStaffTable.id eq id }
                .limit(1)
                .firstOrNull()
                ?.toStaffRecord()
        }

    override suspend fun create(
        email: String,
        passwordHash: String,
        firstName: String?,
        lastName: String?,
        role: String,
    ): StaffRecord = newSuspendedTransaction(Dispatchers.IO) {
        val now = Instant.now()
        val id = SupportStaffTable.insert {
            it[SupportStaffTable.email] = email.trim().lowercase()
            it[SupportStaffTable.passwordHash] = passwordHash
            it[SupportStaffTable.firstName] = firstName?.trim()?.takeIf { value -> value.isNotBlank() }
            it[SupportStaffTable.lastName] = lastName?.trim()?.takeIf { value -> value.isNotBlank() }
            it[SupportStaffTable.role] = role
            it[SupportStaffTable.isActive] = true
            it[SupportStaffTable.createdAt] = now
            it[SupportStaffTable.updatedAt] = null
        }[SupportStaffTable.id]

        StaffRecord(
            id = id,
            email = email.trim().lowercase(),
            passwordHash = passwordHash,
            firstName = firstName?.trim()?.takeIf { it.isNotBlank() },
            lastName = lastName?.trim()?.takeIf { it.isNotBlank() },
            role = role,
            isActive = true,
        )
    }

    private fun ResultRow.toStaffRecord() = StaffRecord(
        id = this[SupportStaffTable.id],
        email = this[SupportStaffTable.email],
        passwordHash = this[SupportStaffTable.passwordHash],
        firstName = this[SupportStaffTable.firstName],
        lastName = this[SupportStaffTable.lastName],
        role = this[SupportStaffTable.role],
        isActive = this[SupportStaffTable.isActive],
    )
}

fun StaffRecord.toPublicDto() = StaffPublicDto(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    role = role,
)
