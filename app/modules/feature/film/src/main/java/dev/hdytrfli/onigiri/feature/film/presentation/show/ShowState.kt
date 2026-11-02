package dev.hdytrfli.onigiri.feature.film.presentation.show

import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidItemPerson
import dev.hdytrfli.onigiri.models.FindroidSeason
import dev.hdytrfli.onigiri.models.FindroidShow

data class ShowState(
    val show: FindroidShow? = null,
    val nextUp: FindroidEpisode? = null,
    val seasons: List<FindroidSeason> = emptyList(),
    val actors: List<FindroidItemPerson> = emptyList(),
    val director: FindroidItemPerson? = null,
    val writers: List<FindroidItemPerson> = emptyList(),
    val error: Exception? = null,
)
