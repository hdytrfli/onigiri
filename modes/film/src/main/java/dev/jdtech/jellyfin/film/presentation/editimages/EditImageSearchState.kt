package dev.jdtech.jellyfin.film.presentation.editimages

import org.jellyfin.sdk.model.api.RemoteImageInfo

/**
 * State for the remote-image search screen: the images the server's providers offer for one image
 * type, and whether the search is still running or failed.
 */
data class EditImageSearchState(
    val isSearching: Boolean = true,
    val results: List<RemoteImageInfo> = emptyList(),
    val error: Exception? = null,
)
