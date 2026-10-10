package dev.jdtech.jellyfin.film.presentation.season

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSeason

data class SeasonState(
    val season: FindroidSeason? = null,
    val episodes: List<FindroidEpisode> = emptyList(),
    val viewMode: String = "list",
    val gridColumnsPortrait: Int = 3,
    val gridColumnsLandscape: Int = 4,
    val error: Exception? = null,
)
