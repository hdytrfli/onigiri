package dev.hdytrfli.onigiri.feature.film.presentation.show

import dev.hdytrfli.onigiri.models.FindroidItem
import java.util.UUID

sealed interface ShowAction {
    data class Play(val startFromBeginning: Boolean = false) : ShowAction

    data class PlayTrailer(val trailer: String) : ShowAction

    data object MarkAsPlayed : ShowAction

    data object UnmarkAsPlayed : ShowAction

    data object MarkAsFavorite : ShowAction

    data object UnmarkAsFavorite : ShowAction

    data object OnBackClick : ShowAction

    data object OnHomeClick : ShowAction

    data class NavigateToItem(val item: FindroidItem) : ShowAction

    data class NavigateToPerson(val personId: UUID) : ShowAction
}
