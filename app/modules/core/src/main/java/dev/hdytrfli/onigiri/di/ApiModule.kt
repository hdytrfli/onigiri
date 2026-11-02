package dev.hdytrfli.onigiri.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.hdytrfli.onigiri.api.JellyfinApi
import dev.hdytrfli.onigiri.api.JellyfinApiFactory
import dev.hdytrfli.onigiri.database.ServerDatabaseDao
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideJellyfinApi(
        appPreferences: AppPreferences,
        database: ServerDatabaseDao,
        jellyfinApiFactory: JellyfinApiFactory,
    ): JellyfinApi {
        val jellyfinApi = jellyfinApiFactory.create()

        val serverId = appPreferences.getValue(appPreferences.currentServer) ?: return jellyfinApi

        // Temporary wrapped in runBlocking to support `database.getServerWithAddressAndUser`
        // suspend function.
        // TODO: move this logic somewhere else.
        runBlocking {
            val serverWithAddressAndUser =
                database.getServerWithAddressAndUser(serverId) ?: return@runBlocking jellyfinApi
            val serverAddress = serverWithAddressAndUser.address ?: return@runBlocking jellyfinApi
            val user = serverWithAddressAndUser.user

            jellyfinApi.apply {
                api.update(baseUrl = serverAddress.address, accessToken = user?.accessToken)
                userId = user?.id
            }
        }

        return jellyfinApi
    }
}
