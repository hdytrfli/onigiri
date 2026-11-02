package dev.hdytrfli.onigiri.di

import android.app.Application
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.hdytrfli.onigiri.database.ServerDatabaseDao
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.utils.Downloader
import dev.hdytrfli.onigiri.utils.DownloaderImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloaderModule {
    @Singleton
    @Provides
    fun provideDownloader(
        application: Application,
        serverDatabase: ServerDatabaseDao,
        jellyfinRepository: JellyfinRepository,
        appPreferences: AppPreferences,
        workManager: WorkManager,
    ): Downloader {
        return DownloaderImpl(
            application,
            serverDatabase,
            jellyfinRepository,
            appPreferences,
            workManager,
        )
    }
}
