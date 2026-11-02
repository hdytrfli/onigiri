package dev.hdytrfli.onigiri.feature.film.presentation.mediadetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.domain.VideoMetadataParser
import dev.hdytrfli.onigiri.models.FindroidItemPerson
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.PersonKind

@HiltViewModel
class MediaDetailViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val videoMetadataParser: VideoMetadataParser,
) : ViewModel() {
    private val _state = MutableStateFlow(MediaDetailState())
    val state = _state.asStateFlow()

    private var loadedKey: String? = null

    fun load(itemId: UUID, isEpisode: Boolean) {
        val key = "$isEpisode:$itemId"
        if (key == loadedKey) return
        loadedKey = key

        viewModelScope.launch {
            _state.emit(MediaDetailState(isLoading = true))
            try {
                val detail =
                    if (isEpisode) {
                        val episode = repository.getEpisode(itemId)
                        // Aggregate series-level cast & crew with episode-level entries.
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
                        MediaDetailState(
                            title = episode.name,
                            overview = episode.overview,
                            year = episode.premiereDate?.year,
                            runtimeMinutes = episode.runtimeTicks / 600_000_000,
                            communityRating = episode.communityRating,
                            people = people,
                            actors = getActors(people),
                            director = getDirector(people),
                            writers = getWriters(people),
                            videoMetadata =
                                episode.sources.firstOrNull()?.let {
                                    videoMetadataParser.parse(it)
                                },
                            backdropUri =
                                (episode.images.backdrop ?: episode.images.showBackdrop)
                                    ?.toString(),
                        )
                    } else {
                        val movie = repository.getMovie(itemId)
                        MediaDetailState(
                            title = movie.name,
                            originalTitle = movie.originalTitle,
                            overview = movie.overview,
                            year = movie.premiereDate?.year,
                            runtimeMinutes = movie.runtimeTicks / 600_000_000,
                            officialRating = movie.officialRating,
                            communityRating = movie.communityRating,
                            genres = movie.genres,
                            people = movie.people,
                            actors = getActors(movie.people),
                            director = getDirector(movie.people),
                            writers = getWriters(movie.people),
                            videoMetadata =
                                movie.sources.firstOrNull()?.let { videoMetadataParser.parse(it) },
                            backdropUri = movie.images.backdrop?.toString(),
                        )
                    }
                _state.emit(detail)
            } catch (e: Exception) {
                _state.emit(MediaDetailState(error = e))
            }
        }
    }

    private suspend fun getActors(people: List<FindroidItemPerson>): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) { people.filter { it.type == PersonKind.ACTOR } }
    }

    private suspend fun getDirector(people: List<FindroidItemPerson>): FindroidItemPerson? {
        return withContext(Dispatchers.Default) {
            people.firstOrNull { it.type == PersonKind.DIRECTOR }
        }
    }

    private suspend fun getWriters(people: List<FindroidItemPerson>): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) { people.filter { it.type == PersonKind.WRITER } }
    }
}
