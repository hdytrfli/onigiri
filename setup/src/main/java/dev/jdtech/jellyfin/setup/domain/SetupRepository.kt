package dev.jdtech.jellyfin.setup.domain

import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import java.util.UUID
import org.jellyfin.sdk.model.api.QuickConnectResult

interface SetupRepository {
    suspend fun getCurrentServer(): Server?

    suspend fun getIsQuickConnectEnabled(): Boolean

    suspend fun initiateQuickConnect(): QuickConnectResult

    suspend fun getQuickConnectState(secret: String): QuickConnectResult

    suspend fun addServer(address: String): Server

    suspend fun login(username: String, password: String)

    suspend fun loginWithSecret(secret: String)

    suspend fun getUsers(serverId: String): List<User>

    suspend fun getCurrentUser(): User?

    suspend fun deleteUser(userId: UUID)

    suspend fun setCurrentUser(userId: UUID)

    /** Clears the active user without deleting the saved profile. */
    suspend fun logout()
}
