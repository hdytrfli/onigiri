package dev.jdtech.jellyfin.film.presentation.editimages

import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ImageType
import timber.log.Timber

/**
 * Runs a remote-image download independently of any screen, so the UI can return to the Edit images
 * page immediately while the server downloads and attaches the image, and show a loading state on
 * the affected type there. [applyingType] is the type currently being downloaded (or null).
 */
@Singleton
class RemoteImageApplyHolder @Inject constructor(private val repository: JellyfinRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _applyingType = MutableStateFlow<ImageType?>(null)
    val applyingType = _applyingType.asStateFlow()

    fun apply(itemId: UUID, type: ImageType, imageUrl: String) {
        scope.launch {
            _applyingType.value = type
            try {
                repository.downloadRemoteImage(itemId = itemId, type = type, imageUrl = imageUrl)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _applyingType.value = null
            }
        }
    }
}
