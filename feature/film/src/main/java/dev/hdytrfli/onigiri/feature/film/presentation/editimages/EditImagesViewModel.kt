package dev.hdytrfli.onigiri.feature.film.presentation.editimages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidSeason
import dev.hdytrfli.onigiri.models.FindroidShow
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType

/**
 * Backs the "Edit images" screen: lists an item's images and lets an admin upload, replace, delete
 * and reorder them. Every mutation reloads the list from the server so the UI always reflects what
 * the server actually stored.
 */
@HiltViewModel
class EditImagesViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val applyHolder: RemoteImageApplyHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(EditImagesState())
    val state = _state.asStateFlow()

    /** The item currently being edited, kept so a finished remote download can trigger a reload. */
    private var currentItemId: UUID? = null

    init {
        viewModelScope.launch {
            var previous: ImageType? = null
            applyHolder.applyingType.collect { type ->
                _state.update { it.copy(downloadingType = type) }
                // A download finishing (type -> null) means a new image may now be attached, so
                // refresh the list even if the user is still on the search screen.
                if (previous != null && type == null) {
                    currentItemId?.let { reload(it) }
                }
                previous = type
            }
        }
    }

    /** Loads the item's images (and its kind, to decide which types to offer). */
    fun load(itemId: UUID) {
        currentItemId = itemId
        viewModelScope.launch { reload(itemId, showLoading = true) }
    }

    /** Uploads [content] as the image of [type] at [index] (or the single image when null). */
    fun upload(
        itemId: UUID,
        type: ImageType,
        index: Int?,
        content: ByteArray,
        mediaType: String,
    ) {
        mutate(itemId, type, index) {
            repository.uploadItemImage(
                itemId = itemId,
                type = type,
                index = index,
                content = content,
                mediaType = mediaType,
            )
        }
    }

    /** Deletes the image of [type] at [index] (or the single image when null). */
    fun delete(itemId: UUID, type: ImageType, index: Int?) {
        mutate(itemId, type, index) { repository.deleteItemImage(itemId, type, index) }
    }

    /** Moves the image of [type] at [index] to the front, making it the primary image. */
    fun makePrimary(itemId: UUID, type: ImageType, index: Int) {
        mutate(itemId, type, index) { repository.updateItemImageIndex(itemId, type, index, 0) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    /** Runs a mutation while marking its slot busy, then reloads the images on success. */
    private fun mutate(
        itemId: UUID,
        type: ImageType,
        index: Int?,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busyKey = imageBusyKey(type, index), error = null) }
            try {
                block()
                reload(itemId)
            } catch (e: Exception) {
                _state.update { it.copy(busyKey = null, error = e) }
            }
        }
    }

    private suspend fun reload(itemId: UUID, showLoading: Boolean = false) {
        if (showLoading) {
            _state.update { it.copy(isLoading = true, error = null) }
        }
        try {
            val images = repository.getItemImages(itemId)
            val kind = loadKind(itemId)
            _state.update {
                it.copy(
                    images = images,
                    kind = kind ?: it.kind,
                    baseUrl = repository.getBaseUrl(),
                    isLoading = false,
                    busyKey = null,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, busyKey = null, error = e) }
        }
    }

    /** Best-effort lookup of the item's kind so the screen can pick the relevant image types. */
    private suspend fun loadKind(itemId: UUID): BaseItemKind? =
        try {
            when (repository.getItem(itemId)) {
                is FindroidMovie -> BaseItemKind.MOVIE
                is FindroidShow -> BaseItemKind.SERIES
                is FindroidSeason -> BaseItemKind.SEASON
                is FindroidEpisode -> BaseItemKind.EPISODE
                else -> null
            }
        } catch (_: Exception) {
            null
        }
}
