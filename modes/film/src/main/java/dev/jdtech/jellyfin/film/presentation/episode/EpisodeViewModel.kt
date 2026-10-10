package dev.jdtech.jellyfin.film.presentation.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.film.domain.VideoMetadataParser
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.PersonKind

@HiltViewModel
class EpisodeViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val videoMetadataParser: VideoMetadataParser,
) : ViewModel() {
    private val _state = MutableStateFlow(EpisodeState())
    val state = _state.asStateFlow()

    lateinit var episodeId: UUID

    fun loadEpisode(episodeId: UUID) {
        this.episodeId = episodeId
        viewModelScope.launch {
            try {
                val episode = repository.getEpisode(episodeId)
                val videoMetadata = videoMetadataParser.parse(episode.sources.first())
                // Aggregate series-level cast & crew with episode-level entries so
                // episodes without their own credits still show the series ones.
                // Episode entries come first, series entries fill in the rest.
                val seriesPeople =
                    try {
                        repository.getShow(episode.seriesId).people
                    } catch (_: Exception) {
                        emptyList()
                    }
                val people =
                    episode.people +
                        seriesPeople.filter { seriesPerson ->
                            episode.people.none { it.id == seriesPerson.id }
                        }
                val actors = getActors(people)
                val director = getDirector(people)
                val writers = getWriters(people)
                val displayExtraInfo = appPreferences.getValue(appPreferences.displayExtraInfo)
                _state.emit(
                    _state.value.copy(
                        episode = episode,
                        videoMetadata = videoMetadata,
                        actors = actors,
                        director = director,
                        writers = writers,
                        displayExtraInfo = displayExtraInfo,
                    )
                )
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private suspend fun getActors(people: List<FindroidItemPerson>): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            people.filter { it.type == PersonKind.ACTOR }
        }
    }

    private suspend fun getDirector(people: List<FindroidItemPerson>): FindroidItemPerson? {
        return withContext(Dispatchers.Default) {
            people.firstOrNull { it.type == PersonKind.DIRECTOR }
        }
    }

    private suspend fun getWriters(people: List<FindroidItemPerson>): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            people.filter { it.type == PersonKind.WRITER }
        }
    }

    fun onAction(action: EpisodeAction) {
        when (action) {
            is EpisodeAction.MarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsPlayed(episodeId)
                    loadEpisode(episodeId)
                }
            }
            is EpisodeAction.UnmarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsUnplayed(episodeId)
                    loadEpisode(episodeId)
                }
            }
            is EpisodeAction.MarkAsFavorite -> {
                viewModelScope.launch {
                    repository.markAsFavorite(episodeId)
                    loadEpisode(episodeId)
                }
            }
            is EpisodeAction.UnmarkAsFavorite -> {
                viewModelScope.launch {
                    repository.unmarkAsFavorite(episodeId)
                    loadEpisode(episodeId)
                }
            }
            else -> Unit
        }
    }
}
