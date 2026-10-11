package com.servicedesk.server.users

import com.servicedesk.server.db.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

interface UserRepository {
    suspend fun listMaxUsers(): List<UserShortDto>
}

class ExposedUserRepository : UserRepository {
    override suspend fun listMaxUsers(): List<UserShortDto> =
        newSuspendedTransaction(Dispatchers.IO) {
            UsersTable
                .selectAll()
                .orderBy(UsersTable.name to SortOrder.ASC)
                .map { row ->
                    UserShortDto(
                        id = row[UsersTable.id],
                        name = row[UsersTable.name],
                        phone = row[UsersTable.phone] ?: "-",
                        city = row[UsersTable.city],
                    )
                }
        }
}
