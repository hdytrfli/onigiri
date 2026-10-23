package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummyCollections
import dev.jdtech.jellyfin.film.presentation.media.MediaAction
import dev.jdtech.jellyfin.film.presentation.media.MediaState
import dev.jdtech.jellyfin.film.presentation.media.MediaViewModel
import dev.jdtech.jellyfin.film.presentation.search.SearchAction
import dev.jdtech.jellyfin.film.presentation.search.SearchState
import dev.jdtech.jellyfin.film.presentation.search.SearchViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.ErrorDialog
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.FilmSearchBar
import dev.jdtech.jellyfin.presentation.film.components.HomeHeader
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

@Composable
fun MediaScreen(
    onItemClick: (FindroidItem) -> Unit,
    onSettingsClick: () -> Unit,
    searchExpanded: Boolean,
    onSearchExpand: (Boolean) -> Unit,
    viewModel: MediaViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadData() }

    MediaScreenLayout(
        state = state,
        searchState = searchState,
        searchExpanded = searchExpanded,
        onSearchExpand = onSearchExpand,
        onSettingsClick = onSettingsClick,
        onAction = { action ->
            when (action) {
                is MediaAction.OnItemClick -> onItemClick(action.item)
                else -> Unit
            }
            viewModel.onAction(action)
        },
        onSearchAction = { action ->
            when (action) {
                is SearchAction.OnItemClick -> onItemClick(action.item)
                else -> Unit
            }
            searchViewModel.onAction(action)
        },
    )
}

@Composable
private fun MediaScreenLayout(
    state: MediaState,
    searchState: SearchState,
    searchExpanded: Boolean,
    onSearchExpand: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onAction: (MediaAction) -> Unit,
    onSearchAction: (SearchAction) -> Unit,
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingTop = safePadding.top + MaterialTheme.spacings.small
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter

    val contentPaddingTop = safePadding.top + 88.dp

    var showErrorDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns(GridSize.THUMBNAIL)),
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = paddingStart,
                    top = contentPaddingTop,
                    end = paddingEnd,
                    bottom = paddingBottom,
                ),
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
            verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            items(state.libraries, key = { it.id }) { library ->
                ItemCard(
                    item = library,
                    direction = Direction.HORIZONTAL,
                    onClick = { onAction(MediaAction.OnItemClick(library)) },
                    modifier = Modifier.animateItem(),
                    fillWidth = true,
                )
            }
        }

        if (searchExpanded) {
            FilmSearchBar(
                state = searchState,
                expanded = searchExpanded,
                onExpand = onSearchExpand,
                onAction = onSearchAction,
                modifier = Modifier.fillMaxWidth(),
                paddingStart = paddingStart,
                paddingEnd = paddingEnd,
            )
        }

        HomeHeader(
            serverName = state.serverName ?: "",
            isLoading = state.isLoading,
            isError = state.error != null,
            onErrorClick = { showErrorDialog = true },
            onRetryClick = { onAction(MediaAction.OnRetryClick) },
            onSearchClick = { onSearchExpand(true) },
            onUserClick = onSettingsClick,
            modifier = Modifier.padding(start = paddingStart, top = paddingTop, end = paddingEnd),
        )

        if (state.error != null && showErrorDialog) {
            ErrorDialog(
                exception = state.error!!,
                onDismissRequest = { showErrorDialog = false },
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun MediaScreenLayoutPreview() {
    FindroidTheme {
        MediaScreenLayout(
            state = MediaState(libraries = dummyCollections, serverName = "Jellyfin"),
            searchState = SearchState(),
            searchExpanded = false,
            onSearchExpand = {},
            onSettingsClick = {},
            onAction = {},
            onSearchAction = {},
        )
    }
}
