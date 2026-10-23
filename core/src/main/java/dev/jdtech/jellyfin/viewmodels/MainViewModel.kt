package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.TabletUiMode
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.Constants
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel
@Inject
constructor(private val appPreferences: AppPreferences, private val database: ServerDatabaseDao) :
    ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val server: Server?, val user: User?) : UiState()

        data object Loading : UiState()
    }

    init {
        check()
    }

    fun loadServerAndUser() {
        viewModelScope.launch {
            val serverId = appPreferences.getValue(appPreferences.currentServer)
            serverId?.let { id ->
                database.getServerWithAddressAndUser(id)?.let { data ->
                    _uiState.emit(UiState.Normal(data.server, data.user))
                }
            }
        }
    }

    /**
     * Continuously derives [MainState] from the underlying preferences and database, so the state
     * (and anything derived from it, e.g. the setup/main navigation condition) reacts to changes as
     * soon as they happen.
     */
    private fun check() {
        viewModelScope.launch {
            _state.emit(MainState(isLoading = true))

            val setupFlags =
                appPreferences.observe(appPreferences.currentServer).flatMapLatest { serverId ->
                    if (serverId == null) {
                        database.observeServersCount().map { serverCount ->
                            SetupFlags(
                                hasServers = serverCount > 0,
                                hasCurrentServer = false,
                                hasCurrentUser = false,
                            )
                        }
                    } else {
                        combine(
                            database.observeServersCount(),
                            database.observeServer(serverId),
                            database.observeServerCurrentUserId(serverId),
                        ) { serverCount, server, currentUserId ->
                            SetupFlags(
                                hasServers = serverCount > 0,
                                hasCurrentServer = server != null,
                                hasCurrentUser = currentUserId != null,
                            )
                        }
                    }
                }

            val interfacePrefs =
                combine(
                        appPreferences.observe(appPreferences.backdropPortraitRatio),
                        appPreferences.observe(appPreferences.backdropLandscapeRatio),
                        appPreferences.observe(appPreferences.confirmExit),
                        appPreferences.observe(appPreferences.hideStatusBar),
                        appPreferences.observe(appPreferences.blurHash),
                    ) { portraitRatio, landscapeRatio, confirmExit, hideStatusBar, blurHash ->
                        InterfacePrefs(
                            portraitRatio,
                            landscapeRatio,
                            confirmExit,
                            hideStatusBar,
                            blurHash,
                            predictiveBack = false,
                            tabletUiMode = null,
                        )
                    }
                    .combine(appPreferences.observe(appPreferences.predictiveBack)) {
                        prefs,
                        predictiveBack ->
                        prefs.copy(predictiveBack = predictiveBack)
                    }
                    .combine(appPreferences.observe(appPreferences.tabletUiMode)) {
                        prefs,
                        tabletUiMode ->
                        prefs.copy(tabletUiMode = tabletUiMode)
                    }

            val titlePrefs =
                combine(
                    appPreferences.observe(appPreferences.titlePortraitWidth),
                    appPreferences.observe(appPreferences.titleLandscapeWidth),
                ) { portraitWidth, landscapeWidth ->
                    TitlePrefs(portraitWidth, landscapeWidth)
                }

            // Layout grid column counts: the two tile sizes, portrait and landscape, plus the gap.
            val gridPrefs =
                combine(
                    appPreferences.observe(appPreferences.gridPosterColumnsPortrait),
                    appPreferences.observe(appPreferences.gridPosterColumnsLandscape),
                    appPreferences.observe(appPreferences.gridThumbnailColumnsPortrait),
                    appPreferences.observe(appPreferences.gridThumbnailColumnsLandscape),
                    appPreferences.observe(appPreferences.gridSpacing),
                ) { posterPortrait, posterLandscape, thumbnailPortrait, thumbnailLandscape, spacing
                    ->
                    GridColumnsPrefs(
                        posterPortrait,
                        posterLandscape,
                        thumbnailPortrait,
                        thumbnailLandscape,
                        spacing,
                    )
                }

            // The home screen suggestions carousel: how many suggestions show at once per
            // orientation, coalesced with the grid prefs so the state build stays one combine.
            val suggestionPrefs =
                combine(
                    appPreferences.observe(appPreferences.gridSuggestionColumnsPortrait),
                    appPreferences.observe(appPreferences.gridSuggestionColumnsLandscape),
                ) { portrait, landscape ->
                    portrait to landscape
                }

            val gridAndSuggestionPrefs =
                gridPrefs.combine(suggestionPrefs) { grid, (suggestionPortrait, suggestionLandscape)
                    ->
                    GridPrefs(
                        grid.posterPortrait,
                        grid.posterLandscape,
                        grid.thumbnailPortrait,
                        grid.thumbnailLandscape,
                        suggestionPortrait,
                        suggestionLandscape,
                        grid.spacing,
                    )
                }

            combine(
                    setupFlags,
                    appPreferences.observe(appPreferences.offlineMode),
                    interfacePrefs,
                    titlePrefs,
                    gridAndSuggestionPrefs,
                ) { flags, offlineMode, interfacePrefs, titlePrefs, gridPrefs ->
                    MainState(
                        isLoading = false,
                        hasServers = flags.hasServers,
                        hasCurrentServer = flags.hasCurrentServer,
                        hasCurrentUser = flags.hasCurrentUser,
                        isOfflineMode = offlineMode,
                        backdropPortraitRatio =
                            interfacePrefs.portraitRatio.toFloatOrNull() ?: 1.5f,
                        backdropLandscapeRatio =
                            interfacePrefs.landscapeRatio.toFloatOrNull() ?: 2.3333333f,
                        contentSpacingScale = Constants.CONTENT_SPACING_SCALE,
                        confirmExit = interfacePrefs.confirmExit,
                        hideStatusBar = interfacePrefs.hideStatusBar,
                        blurHash = interfacePrefs.blurHash,
                        predictiveBack = interfacePrefs.predictiveBack,
                        tabletUiMode = TabletUiMode.fromStorageKey(interfacePrefs.tabletUiMode),
                        titlePortraitWidth = titlePrefs.portraitWidth.toFloatOrNull() ?: 0.7f,
                        titleLandscapeWidth = titlePrefs.landscapeWidth.toFloatOrNull() ?: 0.3f,
                        gridPosterPortrait = gridPrefs.posterPortrait.toIntOrNull() ?: 2,
                        gridPosterLandscape = gridPrefs.posterLandscape.toIntOrNull() ?: 4,
                        gridThumbnailPortrait = gridPrefs.thumbnailPortrait.toIntOrNull() ?: 3,
                        gridThumbnailLandscape = gridPrefs.thumbnailLandscape.toIntOrNull() ?: 4,
                        gridSuggestionPortrait = gridPrefs.suggestionPortrait.toIntOrNull() ?: 1,
                        gridSuggestionLandscape = gridPrefs.suggestionLandscape.toIntOrNull() ?: 3,
                        gridGapScale = gridPrefs.spacing.toFloatOrNull() ?: 1f,
                    )
                }
                .collect { mainState -> _state.emit(mainState) }
        }
    }

    private data class SetupFlags(
        val hasServers: Boolean,
        val hasCurrentServer: Boolean,
        val hasCurrentUser: Boolean,
    )

    private data class TitlePrefs(
        val portraitWidth: String,
        val landscapeWidth: String,
    )

    private data class InterfacePrefs(
        val portraitRatio: String,
        val landscapeRatio: String,
        val confirmExit: Boolean,
        val hideStatusBar: Boolean,
        val blurHash: Boolean,
        val predictiveBack: Boolean,
        val tabletUiMode: String?,
    )

    private data class GridColumnsPrefs(
        val posterPortrait: String,
        val posterLandscape: String,
        val thumbnailPortrait: String,
        val thumbnailLandscape: String,
        val spacing: String,
    )

    private data class GridPrefs(
        val posterPortrait: String,
        val posterLandscape: String,
        val thumbnailPortrait: String,
        val thumbnailLandscape: String,
        val suggestionPortrait: String,
        val suggestionLandscape: String,
        val spacing: String,
    )
}

data class MainState(
    val isLoading: Boolean = true,
    val hasServers: Boolean = false,
    val hasCurrentServer: Boolean = false,
    val hasCurrentUser: Boolean = false,
    val isOfflineMode: Boolean = false,
    val backdropPortraitRatio: Float = 1.5f,
    val backdropLandscapeRatio: Float = 2.3333333f,
    val contentSpacingScale: Float = Constants.CONTENT_SPACING_SCALE,
    val titlePortraitWidth: Float = 0.7f,
    val titleLandscapeWidth: Float = 0.3f,
    val confirmExit: Boolean = false,
    val hideStatusBar: Boolean = false,
    val blurHash: Boolean = true,
    val predictiveBack: Boolean = false,
    val tabletUiMode: TabletUiMode = TabletUiMode.AUTOMATIC,
    val gridPosterPortrait: Int = 2,
    val gridPosterLandscape: Int = 4,
    val gridThumbnailPortrait: Int = 3,
    val gridThumbnailLandscape: Int = 4,
    val gridSuggestionPortrait: Int = 1,
    val gridSuggestionLandscape: Int = 3,
    val gridGapScale: Float = 1f,
)
