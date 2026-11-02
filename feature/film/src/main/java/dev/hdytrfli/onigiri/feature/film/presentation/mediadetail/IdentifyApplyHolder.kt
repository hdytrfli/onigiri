package dev.hdytrfli.onigiri.feature.film.presentation.mediadetail

import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.RemoteSearchResult
import timber.log.Timber

/**
 * Runs an Identify "apply" independently of any screen, so the UI can return to the media detail
 * page immediately while the metadata is applied, and show a loading state there.
 */
@Singleton
class IdentifyApplyHolder @Inject constructor(private val repository: JellyfinRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isApplying = MutableStateFlow(false)
    val isApplying = _isApplying.asStateFlow()

    fun apply(itemId: UUID, result: RemoteSearchResult) {
        scope.launch {
            _isApplying.value = true
            try {
                repository.applyRemoteSearchResult(itemId = itemId, result = result)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _isApplying.value = false
            }
        }
    }
}
