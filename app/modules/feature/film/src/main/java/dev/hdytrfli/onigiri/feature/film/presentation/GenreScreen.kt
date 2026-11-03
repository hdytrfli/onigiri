package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.components.ListOptionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.library.LibraryAction
import dev.hdytrfli.onigiri.feature.film.presentation.library.LibraryViewModel
import dev.hdytrfli.onigiri.models.CollectionType
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.PageSearchField
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.RootPage
import java.util.UUID

/** A genre's media grid, with a floating title bar, search field, and display/sort actions. */
@Composable
fun GenreScreen(
    genreId: UUID,
    genreName: String,
    libraryType: CollectionType,
    onItemClick: (FindroidItem) -> Unit,
    navigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showDisplayDialog by rememberSaveable { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(genreId, libraryType) {
        viewModel.setup(
            parentId = null,
            libraryType = libraryType,
            genreIds = listOf(genreId),
        )
        viewModel.loadItems()
    }
    OfflineModeReload { viewModel.loadItems(force = true) }

    BackHandler(enabled = state.searchExpanded) {
        viewModel.onAction(LibraryAction.SetSearchQuery(""))
        viewModel.onAction(LibraryAction.ToggleSearch)
    }

    val toolbarActions: @Composable RowScope.() -> Unit = {
        PullToRefreshButton(
            state = pullToRefreshState,
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.onAction(LibraryAction.RefreshItems) },
        )
        if (!state.searchExpanded) {
            AppIconButton(
                onClick = { viewModel.onAction(LibraryAction.ToggleSearch) },
                variant = AppButtonVariant.Secondary,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_search),
                        contentDescription = stringResource(CoreR.string.search),
                    )
                },
            )
        }
        ListOptionsMenu(
            onDisplayInfoClick = { showDisplayDialog = true },
            onSortClick = { showSortDialog = true },
        )
    }

    RootPage(
        header = {
            AnimatedContent(
                targetState = state.searchExpanded,
                label = "genre-search",
            ) { searchExpanded ->
                if (searchExpanded) {
                    PageSearchField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onAction(LibraryAction.SetSearchQuery(it)) },
                        onClose = { viewModel.onAction(LibraryAction.ToggleSearch) },
                    )
                } else {
                    PageTopBar(
                        title = genreName.ifBlank { stringResource(CoreR.string.genres) },
                        onBack = navigateBack,
                        trailingContent = toolbarActions,
                        includeTopInset = false,
                    )
                }
            }
        }
    ) {
        LibraryContent(
            state = state,
            onAction = { action ->
                if (action is LibraryAction.OnItemClick) {
                    onItemClick(action.item)
                }
                viewModel.onAction(action)
            },
            showSortDialog = showSortDialog,
            showDisplayDialog = showDisplayDialog,
            onShowSortDialogChange = { showSortDialog = it },
            onShowDisplayDialogChange = { showDisplayDialog = it },
            pullToRefreshState = pullToRefreshState,
            showPullIndicator = false,
            embedded = true,
            sectionTitle = genreName.takeIf { it.isNotBlank() },
            showSearchField = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
