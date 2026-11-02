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
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.User
import dev.hdytrfli.onigiri.models.toFindroidEpisode
import dev.hdytrfli.onigiri.models.toFindroidMovie
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
                val movies =
                    database.getMoviesByServerId(server.id).map {
                        it.toFindroidMovie(database, user.id)
                    }
                val episodes =
                    database.getEpisodesByServerId(server.id).map {
                        it.toFindroidEpisode(database, user.id)
                    }

                syncUserData(jellyfinApi, user, movies)
                syncUserData(jellyfinApi, user, episodes)
            }
        }

        return Result.success()
    }

    private suspend fun syncUserData(
        jellyfinApi: JellyfinApi,
        user: User,
        items: List<FindroidItem>,
    ) {
        for (item in items) {
            val userData = database.getUserDataToBeSynced(user.id, item.id) ?: continue

            try {
                jellyfinApi.itemsApi.updateItemUserData(
                    itemId = item.id,
                    userId = user.id,
                    data =
                        UpdateUserItemDataDto(
                            playbackPositionTicks = userData.playbackPositionTicks,
                            isFavorite = userData.favorite,
                            played = userData.played,
                        ),
                )

                database.setUserDataToBeSynced(user.id, item.id, false)
            } catch (_: Exception) {}
        }
    }
}
