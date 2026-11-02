package dev.hdytrfli.onigiri.feature.film.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.database.ServerDatabaseDao
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.CollectionType
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.HomeItem
import dev.hdytrfli.onigiri.models.HomeSection
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder
import dev.hdytrfli.onigiri.models.UiText
import dev.hdytrfli.onigiri.models.View
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.swr.QueryKeys
import dev.hdytrfli.onigiri.swr.SwrEntry
import dev.hdytrfli.onigiri.swr.SwrStore
import dev.hdytrfli.onigiri.utils.toView
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemKind

/** The whole Home payload is one cache entry so every row appears and refreshes together. */
@Serializable
data class HomeCache(
    val suggestions: List<FindroidItem> = emptyList(),
    val resume: List<FindroidItem> = emptyList(),
    val nextUp: List<FindroidItem> = emptyList(),
    val latestBooks: List<FindroidItem> = emptyList(),
    val latestAudiobooks: List<FindroidItem> = emptyList(),
    val views: List<View> = emptyList(),
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    val repository: JellyfinRepository,
    val appPreferences: AppPreferences,
    val database: ServerDatabaseDao,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val uuidSuggestions = UUID.fromString("31e47044-9b79-4bb0-99d0-0e477ed65420")
    private val uuidLatestBooks = UUID.fromString("6d1f6c1e-0c2b-4a5e-9f1e-3b0f3c5a7d11")
    private val uuidLatestAudiobooks = UUID.fromString("9a8f2b24-51d6-4a3b-8b7f-2f6a4f0c8e33")
    private val uuidContinueWatching =
        UUID(4937169328197226115, -4704919157662094443) // 44845958-8326-4e83-beb4-c4f42e9eeb95
    private val uuidNextUp =
        UUID(1783371395749072194, -6164625418200444295) // 18bfced5-f237-4d42-aa72-d9d7fed19279

    private val uiTextContinueWatching = UiText.StringResource(FilmR.string.continue_watching)
    private val uiTextNextUp = UiText.StringResource(FilmR.string.next_up)
    private val uiTextLatestBooks = UiText.StringResource(FilmR.string.latest_books)
    private val uiTextLatestAudiobooks = UiText.StringResource(FilmR.string.latest_audiobooks)

    private val serializer = HomeCache.serializer()

    init {
        viewModelScope.launch {
            swrStore.observe(QueryKeys.Home, serializer).collect { entry -> applyEntry(entry) }
        }
    }

    /** Entry load: cached rows appear immediately and stale data revalidates in the background. */
    fun refresh() {
        load()
    }

    fun loadData(force: Boolean = false) {
        load(force = force)
    }

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            loadDisplayFields()
            appPreferences.getValue(appPreferences.currentServer)?.let { serverId ->
                loadServerName(serverId)
            }
        }
        swrStore.refresh(QueryKeys.Home, serializer, force = force) { fetchHome() }
    }

    fun retry() {
        load(force = true)
    }

    private suspend fun loadDisplayFields() {
        _state.value =
            _state.value.copy(
                displayFields =
                    ListDisplayField.fromKeys(
                        appPreferences.getValue(appPreferences.listDisplayFields)
                    )
            )
    }

    private suspend fun loadServerName(serverId: String) {
        val server = database.getServer(serverId)
        if (server != null) {
            _state.value = _state.value.copy(server = server)
        }
    }

    private suspend fun applyEntry(entry: SwrEntry<HomeCache>) {
        val cache = entry.data
        if (cache == null) {
            _state.value =
                if (entry.isRefreshing || entry.error != null) {
                    _state.value.copy(
                        isLoading = entry.isRefreshing,
                        error = entry.error as? Exception,
                    )
                } else {
                    _state.value.copy(
                        suggestionsSection = null,
                        resumeSection = null,
                        nextUpSection = null,
                        latestBooksSection = null,
                        latestAudiobooksSection = null,
                        views = emptyList(),
                        isLoading = entry.isRefreshing,
                        error = null,
                    )
                }
            return
        }

        // Preferences can change without invalidating the cached payload, so hide disabled rows
        // here as well as when fetching.
        val showSuggestions = appPreferences.getValue(appPreferences.homeSuggestions)
        val showContinueWatching = appPreferences.getValue(appPreferences.homeContinueWatching)
        val showNextUp = appPreferences.getValue(appPreferences.homeNextUp)
        val showLatest = appPreferences.getValue(appPreferences.homeLatest)

        _state.value =
            _state.value.copy(
                suggestionsSection =
                    cache.suggestions
                        .takeIf { showSuggestions && it.isNotEmpty() }
                        ?.let { HomeItem.Suggestions(id = uuidSuggestions, items = it) },
                resumeSection =
                    cache.resume.toHomeSection(
                        show = showContinueWatching,
                        id = uuidContinueWatching,
                        title = uiTextContinueWatching,
                    ),
                nextUpSection =
                    cache.nextUp.toHomeSection(
                        show = showNextUp,
                        id = uuidNextUp,
                        title = uiTextNextUp,
                    ),
                latestBooksSection =
                    cache.latestBooks.toHomeSection(
                        show = showLatest,
                        id = uuidLatestBooks,
                        title = uiTextLatestBooks,
                    ),
                latestAudiobooksSection =
                    cache.latestAudiobooks.toHomeSection(
                        show = showLatest,
                        id = uuidLatestAudiobooks,
                        title = uiTextLatestAudiobooks,
                    ),
                views =
                    if (showLatest) {
                        cache.views.map { HomeItem.ViewItem(it) }
                    } else {
                        emptyList()
                    },
                isLoading = entry.isRefreshing,
                error = entry.error as? Exception,
            )
    }

    private fun List<FindroidItem>.toHomeSection(
        show: Boolean,
        id: UUID,
        title: UiText,
    ): HomeItem.Section? = takeIf {
        show && it.isNotEmpty()
    }
        ?.let { HomeItem.Section(HomeSection(id, title, it)) }

    private suspend fun fetchHome(): HomeCache = coroutineScope {
        val showSuggestions = appPreferences.getValue(appPreferences.homeSuggestions)
        val showContinueWatching = appPreferences.getValue(appPreferences.homeContinueWatching)
        val showNextUp = appPreferences.getValue(appPreferences.homeNextUp)
        val showLatest = appPreferences.getValue(appPreferences.homeLatest)

        val suggestions = async {
            if (showSuggestions) repository.getSuggestions() else emptyList()
        }
        val resume = async {
            if (showContinueWatching) repository.getResumeItems() else emptyList()
        }
        val nextUp = async { if (showNextUp) repository.getNextUp() else emptyList() }
        val latestBooks = async {
            if (showLatest) {
                repository.getItems(
                    includeTypes = listOf(BaseItemKind.BOOK),
                    recursive = true,
                    sortBy = SortBy.DATE_ADDED,
                    sortOrder = SortOrder.DESCENDING,
                    limit = LATEST_LIMIT,
                )
            } else {
                emptyList()
            }
        }
        val latestAudiobooks = async {
            if (showLatest) {
                repository.getItems(
                    includeTypes = listOf(BaseItemKind.AUDIO_BOOK),
                    recursive = true,
                    sortBy = SortBy.DATE_ADDED,
                    sortOrder = SortOrder.DESCENDING,
                    limit = LATEST_LIMIT,
                )
            } else {
                emptyList()
            }
        }
        val views = async {
            if (showLatest) {
                repository
                    .getUserViews()
                    .filter { view ->
                        val type = CollectionType.fromString(view.collectionType?.serialName)
                        // Board books have their own row, independent of the library type.
                        type in CollectionType.supported && type != CollectionType.Books
                    }
                    .map { view -> view to repository.getLatestMedia(view.id) }
                    .filter { (_, latest) -> latest.isNotEmpty() }
                    .map { (view, latest) -> view.toView(latest) }
            } else {
                emptyList()
            }
        }

        HomeCache(
            suggestions = suggestions.await(),
            resume = resume.await(),
            nextUp = nextUp.await(),
            latestBooks = latestBooks.await(),
            latestAudiobooks = latestAudiobooks.await(),
            views = views.await(),
        )
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnRetryClick -> {
                retry()
            }
            is HomeAction.ToggleDisplayField -> {
                val fields = _state.value.displayFields.toMutableSet()
                if (!fields.add(action.field)) {
                    fields.remove(action.field)
                }
                appPreferences.setValue(
                    appPreferences.listDisplayFields,
                    fields.map { it.key }.toSet(),
                )
                _state.value = _state.value.copy(displayFields = fields)
            }
            else -> Unit
        }
    }

    private companion object {
        const val LATEST_LIMIT = 16
    }
}
