package dev.hdytrfli.onigiri.feature.film.presentation.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidShow
import dev.hdytrfli.onigiri.models.RecommendationKind
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder
import dev.hdytrfli.onigiri.models.UiText
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * The Movies and Shows pages, following the Jellyfin web layout: continue watching, the newly added
 * row, then (movies only) the server's recommendation categories. TV Shows has no recommendation
 * rows in Jellyfin - only continue watching, next up and recently added shows, all shown as series
 * cards rather than episodes.
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
    private var lastOfflineMode: Boolean? = null

    fun load(kind: MediaKind) {
        val offlineMode = appPreferences.getValue(appPreferences.offlineMode)
        if (loadedKind == kind && lastOfflineMode == offlineMode) return
        if (loadedKind != null && loadedKind != kind) {
            _state.value = _state.value.copy(sections = emptyList())
        }
        loadedKind = kind
        lastOfflineMode = offlineMode
        viewModelScope.launch(Dispatchers.Default) {
            _state.emit(
                _state.value.copy(
                    isLoading = true,
                    error = null,
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
                    useThumbnails = true,
                    showRemainingTime = true,
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

        // Continue Watching stays on the episode: the thumbnail and the time-left badge say what
        // is actually being watched, unlike the series poster.
        val resume = repository.getResumeItems().filterIsInstance<FindroidEpisode>()
        if (resume.isNotEmpty()) {
            sections +=
                MediaSection(
                    id = "resume",
                    title = UiText.StringResource(FilmR.string.continue_watching),
                    items = resume,
                    useThumbnails = true,
                    showRemainingTime = true,
                )
        }

        val nextUp = showsFor(repository.getNextUp())
        if (nextUp.isNotEmpty()) {
            sections +=
                MediaSection(
                    id = "next-up",
                    title = UiText.StringResource(FilmR.string.next_up),
                    items = nextUp,
                )
        }

        val latest =
            repository.getItems(
                includeTypes = listOf(BaseItemKind.SERIES),
                recursive = true,
                sortBy = SortBy.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING,
                limit = LATEST_LIMIT,
            )
        if (latest.isNotEmpty()) {
            sections +=
                MediaSection(
                    id = "latest",
                    title = UiText.StringResource(FilmR.string.media_recently_added_shows),
                    items = latest,
                    showsListLink = true,
                )
        }

        return sections
    }

    /**
     * Next Up is an episode list on the server. The Shows page shows the series those episodes
     * belong to, de-duplicated so a show appears only once. (Continue Watching keeps its raw
     * episodes so it can show the episode thumbnail.)
     */
    private suspend fun showsFor(episodes: List<FindroidEpisode>): List<FindroidShow> =
        episodes
            .map { it.seriesId }
            .distinct()
            .mapNotNull { seriesId ->
                try {
                    repository.getShow(seriesId)
                } catch (_: Exception) {
                    null
                }
            }

    private companion object {
        const val LATEST_LIMIT = 18
    }
}
