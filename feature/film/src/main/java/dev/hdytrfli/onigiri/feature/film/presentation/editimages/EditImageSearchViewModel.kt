package dev.hdytrfli.onigiri.feature.film.presentation.editimages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.RemoteImageInfo

/**
 * Backs the remote-image search screen: searches the server's image providers for one image type
 * and applies the chosen result. Applying is fire-and-forget (via [RemoteImageApplyHolder]) so the
 * screen can return to Edit images immediately; the download state is observed by
 * [EditImagesViewModel].
 */
@HiltViewModel
class EditImageSearchViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val applyHolder: RemoteImageApplyHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(EditImageSearchState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<EditImageSearchEvent>()
    val events = eventsChannel.receiveAsFlow()

    /** Searches remote providers for images of [type] to use on [itemId]. */
    fun search(itemId: UUID, type: ImageType) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, results = emptyList(), error = null) }
            try {
                val results = repository.getRemoteImages(itemId, type)
                _state.update { it.copy(isSearching = false, results = results) }
            } catch (e: Exception) {
                _state.update { it.copy(isSearching = false, error = e) }
            }
        }
    }

    /** Applies [result] to [itemId], then signals the screen to return to the Edit images page. */
    fun apply(itemId: UUID, type: ImageType, result: RemoteImageInfo) {
        val imageUrl = result.url
        if (imageUrl == null) {
            viewModelScope.launch { eventsChannel.send(EditImageSearchEvent.Applied) }
            return
        }
        // Fire and forget so the caller can return to Edit images immediately; the loading state is
        // observed via the shared holder.
        applyHolder.apply(itemId, type, imageUrl)
        viewModelScope.launch { eventsChannel.send(EditImageSearchEvent.Applied) }
    }
}

sealed interface EditImageSearchEvent {
    data object Applied : EditImageSearchEvent
}
