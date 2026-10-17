package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.GridId
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
                    appPreferences.observe(appPreferences.contentSpacing),
                    appPreferences.observe(appPreferences.confirmExit),
                    appPreferences.observe(appPreferences.hideStatusBar),
                ) { portraitRatio, landscapeRatio, contentSpacing, confirmExit, hideStatusBar ->
                    InterfacePrefs(
                        portraitRatio,
                        landscapeRatio,
                        contentSpacing,
                        confirmExit,
                        hideStatusBar,
                    )
                }

            val titlePrefs =
                combine(
                    appPreferences.observe(appPreferences.titlePortraitWidth),
                    appPreferences.observe(appPreferences.titleLandscapeWidth),
                    appPreferences.observe(appPreferences.titleImageRatio),
                    appPreferences.observe(appPreferences.naiveTitleOverlay),
                ) { portraitWidth, landscapeWidth, ratio, naive ->
                    TitlePrefs(portraitWidth, landscapeWidth, ratio, naive)
                }

            // One portrait/landscape pair per grid group, in GridId order.
            val gridPrefs =
                GridId.entries.flatMap { gridId ->
                    listOf(
                        appPreferences.observe(appPreferences.gridColumnsPortrait(gridId)),
                        appPreferences.observe(appPreferences.gridColumnsLandscape(gridId)),
                    )
                }
            val gridColumns =
                combine(*gridPrefs.toTypedArray()) { values ->
                    GridId.entries.associateWith { gridId ->
                        val index = GridId.entries.indexOf(gridId) * 2
                        val portrait = values[index].toIntOrNull() ?: gridId.defaultPortrait
                        val landscape = values[index + 1].toIntOrNull() ?: gridId.defaultLandscape
                        portrait to landscape
                    }
                }

            combine(
                    setupFlags,
                    appPreferences.observe(appPreferences.offlineMode),
                    interfacePrefs,
                    titlePrefs,
                    gridColumns,
                ) { flags, offlineMode, interfacePrefs, titlePrefs, gridColumns ->
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
                        contentSpacingScale = interfacePrefs.contentSpacing.toFloatOrNull() ?: 1f,
                        confirmExit = interfacePrefs.confirmExit,
                        hideStatusBar = interfacePrefs.hideStatusBar,
                        titlePortraitWidth = titlePrefs.portraitWidth.toFloatOrNull() ?: 0.7f,
                        titleLandscapeWidth = titlePrefs.landscapeWidth.toFloatOrNull() ?: 0.3f,
                        titleImageRatio = titlePrefs.ratio.toFloatOrNull() ?: 2f,
                        naiveTitleOverlay = titlePrefs.naive,
                        gridColumns = gridColumns,
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
        val ratio: String,
        val naive: Boolean,
    )

    private data class InterfacePrefs(
        val portraitRatio: String,
        val landscapeRatio: String,
        val contentSpacing: String,
        val confirmExit: Boolean,
        val hideStatusBar: Boolean,
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
    val contentSpacingScale: Float = 1f,
    val titlePortraitWidth: Float = 0.7f,
    val titleLandscapeWidth: Float = 0.3f,
    val titleImageRatio: Float = 2f,
    val naiveTitleOverlay: Boolean = false,
    val confirmExit: Boolean = false,
    val hideStatusBar: Boolean = false,
    val gridColumns: Map<GridId, Pair<Int, Int>> = emptyMap(),
)
