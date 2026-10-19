package dev.jdtech.jellyfin.film.presentation.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.RecommendationKind
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * The Movies and Shows pages, following the Jellyfin web layout: continue watching, the newly added
 * row, then (movies only) the server's recommendation categories. TV Shows has no recommendation
 * rows in Jellyfin - only continue watching, latest episodes and next up.
 */
@HiltViewModel
class MediaSuggestionsViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(MediaSuggestionsState())
    val state = _state.asStateFlow()

    private var loadedKind: MediaKind? = null

    fun load(kind: MediaKind) {
        if (loadedKind == kind) return
        loadedKind = kind
        viewModelScope.launch(Dispatchers.Default) {
            _state.emit(
                _state.value.copy(
                    isLoading = true,
                    error = null,
                    displayFields =
                        ListDisplayField.fromKeys(
                            appPreferences.getValue(appPreferences.listDisplayFields)
                        ),
                )
            )
            try {
                val sections =
                    when (kind) {
                        MediaKind.Movies -> loadMovies()
                        MediaKind.Shows -> loadShows()
                    }
                _state.emit(_state.value.copy(sections = sections, isLoading = false))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e, isLoading = false))
            }
        }
    }

    fun retry(kind: MediaKind) {
        loadedKind = null
        load(kind)
    }

    private suspend fun loadMovies(): List<MediaSection> {
        val sections = mutableListOf<MediaSection>()

        val resume = repository.getResumeItems().filterIsInstance<FindroidMovie>()
        if (resume.isNotEmpty()) {
            sections +=
                MediaSection(
                    id = "resume",
                    title = UiText.StringResource(FilmR.string.continue_watching),
                    items = resume,
                )
        }

        val latest =
            repository.getItems(
                includeTypes = listOf(BaseItemKind.MOVIE),
                recursive = true,
                sortBy = SortBy.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING,
                limit = LATEST_LIMIT,
            )
        if (latest.isNotEmpty()) {
            sections +=
                MediaSection(
                    id = "latest",
                    title = UiText.StringResource(FilmR.string.media_recently_added_movies),
                    items = latest,
                    showsListLink = true,
                )
        }

        repository.getMovieRecommendations().forEachIndexed { index, category ->
            val title =
                when (category.kind) {
                    RecommendationKind.BecauseYouWatched ->
                        FilmR.string.recommendation_because_you_watched
                    RecommendationKind.BecauseYouLike ->
                        FilmR.string.recommendation_because_you_like
                    RecommendationKind.DirectedBy -> FilmR.string.recommendation_directed_by
                    RecommendationKind.Starring -> FilmR.string.recommendation_starring
                    RecommendationKind.Unknown -> null
                }
            if (title != null && category.items.isNotEmpty()) {
                sections +=
                    MediaSection(
                        id = "recommendation-$index",
                        title = UiText.StringResource(title, category.baselineName.orEmpty()),
                        items = category.items,
                    )
            }
        }

        return sections
    }

    private suspend fun loadShows(): List<MediaSection> {
        val sections = mutableListOf<MediaSection>()

        val resume = repository.getResumeItems().filterIsInstance<FindroidEpisode>()
        if (resume.isNotEmpty()) {
            sections +=
                MediaSection(
                    id = "resume",
                    title = UiText.StringResource(FilmR.string.continue_watching),
                    items = resume,
                )
        }

        val latest =
            repository.getItems(
                includeTypes = listOf(BaseItemKind.EPISODE),
                recursive = true,
                sortBy = SortBy.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING,
                limit = LATEST_EPISODES_LIMIT,
            )
        if (latest.isNotEmpty()) {
            sections +=
                MediaSection(
                    id = "latest",
                    title = UiText.StringResource(FilmR.string.media_recently_added_episodes),
                    items = latest,
                    showsListLink = true,
                )
        }

        val nextUp = repository.getNextUp()
        if (nextUp.isNotEmpty()) {
            sections +=
                MediaSection(
                    id = "next-up",
                    title = UiText.StringResource(FilmR.string.next_up),
                    items = nextUp,
                )
        }

        return sections
    }

    private companion object {
        const val LATEST_LIMIT = 18
        const val LATEST_EPISODES_LIMIT = 30
    }
}
