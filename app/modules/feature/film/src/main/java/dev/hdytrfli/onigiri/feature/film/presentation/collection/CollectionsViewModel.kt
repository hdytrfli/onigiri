package dev.hdytrfli.onigiri.feature.film.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.feature.film.presentation.media.MediaKind
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidShow
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.swr.QueryKeys
import dev.hdytrfli.onigiri.swr.SwrEntry
import dev.hdytrfli.onigiri.swr.SwrStore
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemKind

@Serializable data class CollectionsCache(val items: List<FindroidItem> = emptyList())

data class CollectionsState(
    val items: List<FindroidItem> = emptyList(),
    val displayFields: Set<ListDisplayField> = emptySet(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

/**
 * The Collections tab groups box sets by what they contain: Jellyfin's box-set DTO carries no
 * movie/show kind, so each set is classified by its first movie or series child.
 */
@HiltViewModel
class CollectionsViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(CollectionsState())
    val state = _state.asStateFlow()

    private val serializer = CollectionsCache.serializer()
    private var currentKind: MediaKind? = null
    private var observeJob: Job? = null

    fun load(kind: MediaKind, force: Boolean = false) {
        _state.value =
            _state.value.copy(
                displayFields =
                    ListDisplayField.fromKeys(
                        appPreferences.getValue(appPreferences.listDisplayFields)
                    )
            )
        observe(kind)
        swrStore.refresh(QueryKeys.collections(kind.name), serializer, force = force) {
            fetchCollections(kind)
        }
    }

    fun retry(kind: MediaKind) {
        load(kind, force = true)
    }

    private fun observe(kind: MediaKind) {
        if (currentKind == kind) return
        currentKind = kind
        _state.value = _state.value.copy(items = emptyList(), isLoading = true, error = null)
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            swrStore.observe(QueryKeys.collections(kind.name), serializer).collect(::applyEntry)
        }
    }

    private fun applyEntry(entry: SwrEntry<CollectionsCache>) {
        _state.value =
            _state.value.copy(
                items =
                    entry.data?.items
                        ?: if (entry.isRefreshing || entry.error != null) {
                            _state.value.items
                        } else {
                            emptyList()
                        },
                isLoading = entry.isRefreshing || (entry.data == null && entry.error == null),
                error = entry.error as? Exception,
            )
    }

    private suspend fun fetchCollections(kind: MediaKind): CollectionsCache {
        val boxSets =
            repository.getItems(
                includeTypes = listOf(BaseItemKind.BOX_SET),
                recursive = true,
                sortBy = SortBy.NAME,
            )
        val items = mutableListOf<FindroidItem>()
        for (boxSet in boxSets) {
            val first =
                repository
                    .getItems(
                        parentId = boxSet.id,
                        includeTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                        recursive = false,
                        limit = 1,
                    )
                    .firstOrNull()
            val matches =
                when (kind) {
                    MediaKind.Movies -> first is FindroidMovie
                    MediaKind.Shows -> first is FindroidShow
                }
            if (matches) {
                items += boxSet
            }
        }
        return CollectionsCache(items = items)
    }
}
