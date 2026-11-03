package dev.hdytrfli.onigiri.feature.film.presentation.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.buildItemImageUrl
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.SearchHint

enum class HomeSearchGroupType {
    PEOPLE,
    MOVIES,
    SHOWS,
    EPISODES,
    SEASONS,
    BOOKS,
    AUDIOBOOKS,
    SONGS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    COLLECTIONS,
    VIDEOS,
}

data class HomeSearchResult(
    val id: UUID,
    val name: String,
    val kind: BaseItemKind?,
    val subtitle: String?,
    val image: Uri?,
) {
    val isPerson: Boolean
        get() = kind == BaseItemKind.PERSON
}

data class HomeSearchGroup(
    val type: HomeSearchGroupType,
    val results: List<HomeSearchResult>,
)

data class HomeSearchState(
    val query: String = "",
    val loading: Boolean = false,
    val groups: List<HomeSearchGroup> = emptyList(),
    val error: Throwable? = null,
)

@HiltViewModel
class HomeSearchViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(HomeSearchState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null
    private var lastQuery = ""

    fun search(query: String) {
        lastQuery = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _state.value = HomeSearchState()
            return
        }

        searchJob = viewModelScope.launch {
            _state.value = HomeSearchState(query = query, loading = true)
            delay(300)

            try {
                val hints = repository.getSearchHints(query)
                _state.value =
                    HomeSearchState(
                        query = query,
                        groups = groupHints(hints, repository.getBaseUrl()),
                    )
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: Exception) {
                _state.value = HomeSearchState(query = query, error = e)
            }
        }
    }

    fun retry() {
        if (lastQuery.isNotBlank()) search(lastQuery)
    }

    private fun groupHints(hints: List<SearchHint>, baseUrl: String): List<HomeSearchGroup> {
        val results = hints.mapNotNull { it.toResult(baseUrl) }.distinctBy { it.id to it.kind }

        val grouped = results.groupBy { it.kind.toGroupType() }
        return HomeSearchGroupType.entries.mapNotNull { type ->
            grouped[type]?.takeIf { it.isNotEmpty() }?.let { HomeSearchGroup(type, it) }
        }
    }
}

private fun SearchHint.toResult(baseUrl: String): HomeSearchResult? {
    val itemId = itemId ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    val image =
        primaryImageTag?.let { buildItemImageUrl(baseUrl, itemId, ImageType.PRIMARY, tag = it) }
            ?: thumbImageTag?.let { tag ->
                thumbImageItemId
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?.let { buildItemImageUrl(baseUrl, it, ImageType.THUMB, tag = tag) }
            }

    return HomeSearchResult(
        id = itemId,
        name = name,
        kind = type,
        subtitle = subtitle(),
        image = image,
    )
}

private fun SearchHint.subtitle(): String? {
    val episodeIndex =
        listOfNotNull(
                parentIndexNumber?.let { "S$it" },
                indexNumber?.let { "E$it" },
            )
            .joinToString(" ")

    return when (type) {
        BaseItemKind.EPISODE ->
            listOfNotNull(
                    series,
                    episodeIndex.takeIf { it.isNotBlank() },
                    productionYear?.toString(),
                )
                .joinToString(" · ")
                .ifBlank { null }
        BaseItemKind.SEASON ->
            listOfNotNull(series, indexNumber?.let { "Season $it" }).joinToString(" · ").ifBlank {
                null
            }
        BaseItemKind.AUDIO ->
            listOfNotNull(album, artists?.joinToString(", ")).joinToString(" · ").ifBlank { null }
        BaseItemKind.MUSIC_ALBUM ->
            listOfNotNull(albumArtist ?: artists?.joinToString(", "), productionYear?.toString())
                .joinToString(" · ")
                .ifBlank { null }
        BaseItemKind.MUSIC_ARTIST -> null
        else -> productionYear?.toString()
    }
}

private fun BaseItemKind?.toGroupType(): HomeSearchGroupType? =
    when (this) {
        BaseItemKind.PERSON -> HomeSearchGroupType.PEOPLE
        BaseItemKind.MOVIE -> HomeSearchGroupType.MOVIES
        BaseItemKind.SERIES -> HomeSearchGroupType.SHOWS
        BaseItemKind.EPISODE -> HomeSearchGroupType.EPISODES
        BaseItemKind.SEASON -> HomeSearchGroupType.SEASONS
        BaseItemKind.BOOK -> HomeSearchGroupType.BOOKS
        BaseItemKind.AUDIO_BOOK -> HomeSearchGroupType.AUDIOBOOKS
        BaseItemKind.AUDIO -> HomeSearchGroupType.SONGS
        BaseItemKind.MUSIC_ALBUM -> HomeSearchGroupType.ALBUMS
        BaseItemKind.MUSIC_ARTIST -> HomeSearchGroupType.ARTISTS
        BaseItemKind.PLAYLIST -> HomeSearchGroupType.PLAYLISTS
        BaseItemKind.BOX_SET -> HomeSearchGroupType.COLLECTIONS
        BaseItemKind.VIDEO,
        BaseItemKind.MUSIC_VIDEO,
        BaseItemKind.TRAILER,
        BaseItemKind.PROGRAM,
        BaseItemKind.LIVE_TV_PROGRAM,
        BaseItemKind.RECORDING -> HomeSearchGroupType.VIDEOS
        else -> null
    }
