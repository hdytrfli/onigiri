package dev.hdytrfli.onigiri.api

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds [JellyfinApi] instances with the user's network settings.
 *
 * The app keeps one API instance for the UI, but background workers need their own instance so
 * changing the shared base URL or access token can never race with a screen.
 */
@Singleton
class JellyfinApiFactory
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) {
    fun create(): JellyfinApi =
        JellyfinApi(
            androidContext = context,
            requestTimeout = appPreferences.getValue(appPreferences.requestTimeout),
            connectTimeout = appPreferences.getValue(appPreferences.connectTimeout),
            socketTimeout = appPreferences.getValue(appPreferences.socketTimeout),
        )
}
