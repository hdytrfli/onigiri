package dev.jdtech.jellyfin

import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

/**
 * The onboarding rework introduces a single-server, single-session model. Existing installations
 * keep their downloaded files, but their old server/users/tokens are discarded once so the new
 * onboarding can take over cleanly.
 *
 * This is also what makes the upgrade behave like a logout: no old access token survives into the
 * new session, while downloaded media remains usable after the new server is configured.
 */
@Singleton
class OnboardingSessionMigration
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val database: ServerDatabaseDao,
    private val jellyfinApi: JellyfinApi,
) {
    fun run() {
        if (
            appPreferences.getValue(appPreferences.onboardingComplete) ||
                appPreferences.getValue(appPreferences.onboardingSessionCleared)
        ) {
            return
        }

        runBlocking {
            database.deleteAllUsers()
            database.deleteAllServers()
            database.clearQueryCache()
            appPreferences.setValue(appPreferences.currentServer, null)
            appPreferences.setValue(appPreferences.onboardingSessionCleared, true)
            jellyfinApi.api.update(accessToken = null)
            jellyfinApi.userId = null
        }
    }
}
