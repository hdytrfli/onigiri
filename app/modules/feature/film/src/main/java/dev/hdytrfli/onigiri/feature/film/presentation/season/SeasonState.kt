package dev.hdytrfli.onigiri.feature.film.presentation.season

import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidSeason

data class SeasonState(
    val season: FindroidSeason? = null,
    val episodes: List<FindroidEpisode> = emptyList(),
    val error: Exception? = null,
)
