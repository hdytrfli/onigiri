package dev.hdytrfli.onigiri.feature.film.presentation.media

import dev.hdytrfli.onigiri.models.FindroidCollection

sealed interface MediaAction {
    data class OnItemClick(val item: FindroidCollection) : MediaAction

    data object OnFavoritesClick : MediaAction

    data object OnRetryClick : MediaAction
}
