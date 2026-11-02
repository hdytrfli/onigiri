package dev.hdytrfli.onigiri.feature.film.presentation.collection

import dev.hdytrfli.onigiri.models.FindroidItem

sealed interface CollectionAction {
    data class OnItemClick(val item: FindroidItem) : CollectionAction

    data object OnBackClick : CollectionAction
}
