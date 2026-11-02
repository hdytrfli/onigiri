package dev.hdytrfli.onigiri.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppPreferencesModule {
    @Singleton
    @Provides
    fun provideAppPreferences(
        @ApplicationContext context: Context,
        sp: SharedPreferences,
    ): AppPreferences {
        return AppPreferences(sp, context)
    }
}
