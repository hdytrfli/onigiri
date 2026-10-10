package dev.jdtech.jellyfin

import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.setup.domain.SetupRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Debug-only convenience that connects to a server and logs in automatically, so repeat local test
 * builds don't need manual setup. Values come from `gradle.properties` (`findroid.debugAutoLogin*`)
 * and are compiled into the debug APK only. No-op if any value is blank or if a server + user are
 * already configured.
 */
@Singleton
class DebugAutoLogin
@Inject
constructor(
    private val setupRepository: SetupRepository,
    private val appPreferences: AppPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun run() {
        if (!BuildConfig.DEBUG) return

        val url = BuildConfig.DEBUG_AUTO_LOGIN_URL
        val username = BuildConfig.DEBUG_AUTO_LOGIN_USERNAME
        val password = BuildConfig.DEBUG_AUTO_LOGIN_PASSWORD
        if (url.isBlank() || username.isBlank() || password.isBlank()) return

        scope.launch {
            try {
                if (
                    appPreferences.getValue(appPreferences.currentServer) != null &&
                        setupRepository.getCurrentUser() != null
                ) {
                    Timber.d("Debug auto-login: already configured, skipping")
                    return@launch
                }

                Timber.d("Debug auto-login: connecting to $url")
                val server = setupRepository.addServer(url)
                appPreferences.setValue(appPreferences.currentServer, server.id)
                setupRepository.login(username, password)
                Timber.d("Debug auto-login: success")
            } catch (e: Exception) {
                Timber.w(e, "Debug auto-login failed")
            }
        }
    }
}
