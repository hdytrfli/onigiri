package dev.jdtech.jellyfin.film.presentation.mediadetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.film.domain.VideoMetadataParser
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
}
