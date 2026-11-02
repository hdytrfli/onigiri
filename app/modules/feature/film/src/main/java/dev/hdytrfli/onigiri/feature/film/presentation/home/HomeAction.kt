package dev.hdytrfli.onigiri.feature.film.presentation.home

import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.FindroidCollection
import dev.hdytrfli.onigiri.models.FindroidItem

sealed interface HomeAction {
    data class OnItemClick(val item: FindroidItem) : HomeAction

    data class OnLibraryClick(val library: FindroidCollection) : HomeAction

    data class ToggleDisplayField(val field: ListDisplayField) : HomeAction

    data object OnRetryClick : HomeAction

    data object OnSearchClick : HomeAction

    data object OnSettingsClick : HomeAction
}
