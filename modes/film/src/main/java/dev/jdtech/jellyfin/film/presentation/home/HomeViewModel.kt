package dev.jdtech.jellyfin.film.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.toView
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    val repository: JellyfinRepository,
    val appPreferences: AppPreferences,
    val database: ServerDatabaseDao,
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

    private var hasLoadedOnce = false
    private var lastOfflineMode: Boolean? = null

    /**
     * Loads everything on first entry. On later entries (coming back from a detail page) it
     * refreshes the sections but leaves the suggestions alone, so the carousel keeps its items and
     * scroll position. Use [loadData] for a full refresh.
     */
    fun refresh() {
        val offlineMode = appPreferences.getValue(appPreferences.offlineMode)
        if (!hasLoadedOnce || lastOfflineMode != offlineMode) {
            hasLoadedOnce = true
            loadData()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                loadDisplayFields()
                loadResumeItems()
                loadNextUpItems()
                loadLatestBooks()
                loadLatestAudiobooks()
                loadViews()
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    fun loadData() {
        Timber.i("Loading data")
        lastOfflineMode = appPreferences.getValue(appPreferences.offlineMode)
        viewModelScope.launch(Dispatchers.Default) {
            _state.emit(_state.value.copy(isLoading = true, error = null))
            try {
                appPreferences.getValue(appPreferences.currentServer)?.let { serverId ->
                    loadServerName(serverId)
                }

                loadDisplayFields()
                loadSuggestions()
                loadResumeItems()
                loadNextUpItems()
                loadLatestBooks()
                loadLatestAudiobooks()
                loadViews()
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
            _state.emit(_state.value.copy(isLoading = false))
        }
    }

    private suspend fun loadDisplayFields() {
        _state.emit(
            _state.value.copy(
                displayFields =
                    ListDisplayField.fromKeys(
                        appPreferences.getValue(appPreferences.listDisplayFields)
                    )
            )
        )
    }

    private suspend fun loadServerName(serverId: String) {
        val server = database.getServer(serverId)
        if (server != null) {
            _state.emit(_state.value.copy(server = server))
        }
    }

    private suspend fun loadSuggestions() {
        Timber.i("Loading suggestions")
        if (!appPreferences.getValue(appPreferences.homeSuggestions)) {
            _state.emit(_state.value.copy(suggestionsSection = null))
            return
        }

        val items = repository.getSuggestions()

        val section =
            if (items.isEmpty()) {
                null
            } else {
                HomeItem.Suggestions(id = uuidSuggestions, items = items)
            }

        _state.emit(_state.value.copy(suggestionsSection = section))
    }

    private suspend fun loadResumeItems() {
        Timber.i("Loading resume items")
        if (!appPreferences.getValue(appPreferences.homeContinueWatching)) {
            _state.emit(_state.value.copy(resumeSection = null))
            return
        }

        val resumeItems = repository.getResumeItems()

        val section =
            if (resumeItems.isEmpty()) {
                null
            } else {
                HomeItem.Section(
                    HomeSection(uuidContinueWatching, uiTextContinueWatching, resumeItems)
                )
            }

        _state.emit(_state.value.copy(resumeSection = section))
    }

    private suspend fun loadNextUpItems() {
        Timber.i("Loading next up items")
        if (!appPreferences.getValue(appPreferences.homeNextUp)) {
            _state.emit(_state.value.copy(nextUpSection = null))
            return
        }

        val nextUpItems = repository.getNextUp()

        val section =
            if (nextUpItems.isEmpty()) {
                null
            } else {
                HomeItem.Section(HomeSection(uuidNextUp, uiTextNextUp, nextUpItems))
            }

        _state.emit(_state.value.copy(nextUpSection = section))
    }

    /**
     * The newest books across every library, whether the server typed the library as Books or not.
     */
    private suspend fun loadLatestBooks() {
        Timber.i("Loading latest books")
        if (!appPreferences.getValue(appPreferences.homeLatest)) {
            _state.emit(_state.value.copy(latestBooksSection = null))
            return
        }

        val books =
            repository.getItems(
                includeTypes = listOf(BaseItemKind.BOOK),
                recursive = true,
                sortBy = SortBy.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING,
                limit = 16,
            )

        val section =
            if (books.isEmpty()) {
                null
            } else {
                HomeItem.Section(HomeSection(uuidLatestBooks, uiTextLatestBooks, books))
            }

        _state.emit(_state.value.copy(latestBooksSection = section))
    }

    private suspend fun loadLatestAudiobooks() {
        Timber.i("Loading latest audiobooks")
        if (!appPreferences.getValue(appPreferences.homeLatest)) {
            _state.emit(_state.value.copy(latestAudiobooksSection = null))
            return
        }

        val audiobooks =
            repository.getItems(
                includeTypes = listOf(BaseItemKind.AUDIO_BOOK),
                recursive = true,
                sortBy = SortBy.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING,
                limit = 16,
            )

        val section =
            if (audiobooks.isEmpty()) {
                null
            } else {
                HomeItem.Section(
                    HomeSection(uuidLatestAudiobooks, uiTextLatestAudiobooks, audiobooks)
                )
            }

        _state.emit(_state.value.copy(latestAudiobooksSection = section))
    }

    private suspend fun loadViews() {
        Timber.i("Loading views")
        val items =
            if (appPreferences.getValue(appPreferences.homeLatest)) {
                repository
                    .getUserViews()
                    .filter { view ->
                        val type = CollectionType.fromString(view.collectionType?.serialName)
                        // Board books in their own row, independent of the library type.
                        type in CollectionType.supported && type != CollectionType.Books
                    }
                    .map { view -> view to repository.getLatestMedia(view.id) }
                    .filter { (_, latest) -> latest.isNotEmpty() }
                    .map { (view, latest) -> view.toView(latest) }
                    .map { HomeItem.ViewItem(it) }
            } else {
                emptyList()
            }

        _state.emit(_state.value.copy(views = items))
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnRetryClick -> {
                loadData()
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
}
