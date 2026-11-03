package dev.hdytrfli.onigiri.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.hdytrfli.onigiri.api.JellyfinApi
import dev.hdytrfli.onigiri.api.JellyfinApiFactory
import dev.hdytrfli.onigiri.database.ServerDatabaseDao
import dev.hdytrfli.onigiri.models.User
import org.jellyfin.sdk.model.api.UpdateUserItemDataDto

@HiltWorker
class SyncWorker
@AssistedInject
constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    val database: ServerDatabaseDao,
    val jellyfinApiFactory: JellyfinApiFactory,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val jellyfinApi = jellyfinApiFactory.create()

        val servers = database.getServers()

        for (server in servers) {
            val serverWithAddressesAndUsers =
                database.getServerWithAddressesAndUsers(server.id) ?: continue
            val serverAddress =
                serverWithAddressesAndUsers.addresses.firstOrNull {
                    it.id == server.currentServerAddressId
                } ?: continue
            for (user in serverWithAddressesAndUsers.users) {
                jellyfinApi.apply {
                    api.update(baseUrl = serverAddress.address, accessToken = user.accessToken)
                    userId = user.id
                }
                syncUserData(jellyfinApi, user)
            }
        }

        return Result.success()
    }

    private suspend fun syncUserData(jellyfinApi: JellyfinApi, user: User) {
        for (userData in database.getUserDataToBeSynced(user.id)) {
            try {
                jellyfinApi.itemsApi.updateItemUserData(
                    itemId = userData.itemId,
                    userId = user.id,
                    data =
                        UpdateUserItemDataDto(
                            playbackPositionTicks = userData.playbackPositionTicks,
                            isFavorite = userData.favorite,
                            played = userData.played,
                        ),
                )

                database.setUserDataToBeSynced(user.id, userData.itemId, false)
            } catch (_: Exception) {}
        }
    }
}
