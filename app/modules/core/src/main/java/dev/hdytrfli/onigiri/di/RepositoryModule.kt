package dev.hdytrfli.onigiri.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.hdytrfli.onigiri.api.JellyfinApi
import dev.hdytrfli.onigiri.database.ServerDatabaseDao
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.repository.JellyfinRepositoryImpl
import dev.hdytrfli.onigiri.repository.JellyfinRepositoryOfflineImpl
import dev.hdytrfli.onigiri.repository.RoutingJellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideJellyfinRepositoryImpl(
        application: Application,
        jellyfinApi: JellyfinApi,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
    ): JellyfinRepositoryImpl {
        return JellyfinRepositoryImpl(application, jellyfinApi, serverDatabase, appPreferences)
    }

    @Singleton
    @Provides
    fun provideJellyfinRepositoryOfflineImpl(
        application: Application,
        jellyfinApi: JellyfinApi,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
    ): JellyfinRepositoryOfflineImpl {
        return JellyfinRepositoryOfflineImpl(
            application,
            jellyfinApi,
            serverDatabase,
            appPreferences,
        )
    }

    @Singleton
    @Provides
    fun provideJellyfinRepository(
        jellyfinRepositoryImpl: JellyfinRepositoryImpl,
        jellyfinRepositoryOfflineImpl: JellyfinRepositoryOfflineImpl,
        appPreferences: AppPreferences,
    ): JellyfinRepository {
        return RoutingJellyfinRepository(
            online = jellyfinRepositoryImpl,
            offline = jellyfinRepositoryOfflineImpl,
            appPreferences = appPreferences,
        )
    }
}
