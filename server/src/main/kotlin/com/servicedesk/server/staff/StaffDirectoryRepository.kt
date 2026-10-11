package com.servicedesk.server.staff

import com.servicedesk.server.db.SupportStaffTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

interface StaffDirectoryRepository {
    suspend fun listActive(): List<StaffBriefDto>
}

class ExposedStaffDirectoryRepository : StaffDirectoryRepository {
    override suspend fun listActive(): List<StaffBriefDto> =
        newSuspendedTransaction(Dispatchers.IO) {
            SupportStaffTable
                .selectAll()
                .where { SupportStaffTable.isActive eq true }
                .orderBy(SupportStaffTable.email to SortOrder.ASC)
                .map { row ->
                    StaffBriefDto(
                        id = row[SupportStaffTable.id],
                        email = row[SupportStaffTable.email],
                        firstName = row[SupportStaffTable.firstName],
                        lastName = row[SupportStaffTable.lastName],
                        role = row[SupportStaffTable.role],
                    )
                }
        }
}
