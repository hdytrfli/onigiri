package dev.hdytrfli.onigiri.feature.film.presentation.episode

import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidItemPerson
import dev.hdytrfli.onigiri.models.VideoMetadata

data class EpisodeState(
    val episode: FindroidEpisode? = null,
    val videoMetadata: VideoMetadata? = null,
    val actors: List<FindroidItemPerson> = emptyList(),
    val director: FindroidItemPerson? = null,
    val writers: List<FindroidItemPerson> = emptyList(),
    val moreFromSeason: List<FindroidEpisode> = emptyList(),
    val seasonName: String? = null,
    val showRecommendations: Boolean = true,
    val displayExtraInfo: Boolean = false,
    val error: Exception? = null,
)
