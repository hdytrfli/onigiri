package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.BooksRoute
import dev.hdytrfli.onigiri.LocalTabReselect
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.books.BOOK_SORT_OPTIONS
import dev.hdytrfli.onigiri.feature.film.presentation.books.BooksViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.components.Direction
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCard
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCountBadge
import dev.hdytrfli.onigiri.feature.film.presentation.components.ListOptionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.SortByDialog
import dev.hdytrfli.onigiri.feature.film.presentation.components.section
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.FindroidBook
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.PageSearchField
import dev.hdytrfli.onigiri.ui.components.PageToolbar
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.TabContent
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull

/** The Books root page, split into books and audiobooks. */
@Composable
fun BooksScreen(
    onItemClick: (FindroidItem) -> Unit,
    viewModel: BooksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSortDialog by remember { mutableStateOf(false) }
    var showDisplayDialog by remember { mutableStateOf(false) }
    val safePadding = rememberSafePadding()
    val pullToRefreshState = rememberPullToRefreshState()

    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
        searchQuery = ""
    }

    // Pressing the Books item again cycles Books <-> Audiobooks.
    val tabReselect = LocalTabReselect.current
    LaunchedEffect(tabReselect) {
        tabReselect.reselects(BooksRoute).collect { selectedTab = (selectedTab + 1) % 2 }
    }

    LaunchedEffect(tabReselect) {
        tabReselect.booksTab.filterNotNull().collect { tab ->
            selectedTab = tab
            tabReselect.consumeBooksTab()
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.loadIfNeeded()
        viewModel.refreshOpenedItem()
        onPauseOrDispose {}
    }
    OfflineModeReload { viewModel.load(force = true) }

    TopBarPageShell(
        topBar = {
            AnimatedContent(targetState = searchExpanded, label = "books-search") { expanded ->
                if (expanded) {
                    PageSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        onClose = {
                            searchExpanded = false
                            searchQuery = ""
                        },
                    )
                } else {
                    PageToolbar(
                        tabs =
                            listOf(
                                stringResource(CoreR.string.books_label),
                                stringResource(CoreR.string.audiobooks_label),
                            ),
                        tabIcons =
                            listOf(CoreR.drawable.ic_book_open, CoreR.drawable.ic_headphones),
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it },
                        dropdownOnCompact = true,
                        actions = {
                            PullToRefreshButton(
                                state = pullToRefreshState,
                                isRefreshing = state.isLoading,
                                onRefresh = { viewModel.retry() },
                            )
                            AppIconButton(
                                onClick = { searchExpanded = true },
                                content = {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_search),
                                        contentDescription = stringResource(CoreR.string.search),
                                    )
                                },
                            )
                            ListOptionsMenu(
                                onDisplayInfoClick = { showDisplayDialog = true },
                                onSortClick = { showSortDialog = true },
                            )
                        },
                    )
                }
            }
        }
    ) {
        TabContent(selectedTab = selectedTab) { tab ->
            val audiobooks = tab == 1
            val sectionTitle =
                stringResource(
                    if (audiobooks) CoreR.string.audiobooks_label else CoreR.string.books_label
                )
            val items =
                state.items.values
                    .flatten()
                    .filter { item -> item is FindroidBook && item.isAudiobook == audiobooks }
                    .distinctBy { it.id }
                    .filter { item ->
                        searchQuery.isBlank() || item.name.contains(searchQuery, true)
                    }
            val contentPadding =
                PaddingValues(
                    start = safePadding.start + MaterialTheme.spacings.gutter,
                    top = LocalRootPageContentInset.current,
                    end = safePadding.end + MaterialTheme.spacings.gutter,
                    bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
                )

            AppPullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.retry() },
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                showIndicator = false,
            ) {
                if (state.error != null && items.isEmpty()) {
                    FullPageError(
                        onRefresh = { viewModel.retry() },
                        isRefreshing = state.isLoading,
                        error = state.error,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    EmptyStateOverlay(
                        visible = items.isEmpty(),
                        loading = state.isLoading,
                        context =
                            stringResource(
                                if (audiobooks) {
                                    CoreR.string.empty_context_audiobooks
                                } else {
                                    CoreR.string.empty_context_books
                                }
                            ),
                        onRefresh = { viewModel.retry() },
                        title = sectionTitle,
                        contentPadding = contentPadding,
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                            verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                        ) {
                            if (items.isNotEmpty()) {
                                section(
                                    title = sectionTitle,
                                    trailingContent = {
                                        ItemCountBadge(
                                            count = items.size,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    },
                                ) {
                                    items(items = items, key = { it.id }) { item ->
                                        ItemCard(
                                            item = item,
                                            direction = Direction.VERTICAL,
                                            onClick = {
                                                viewModel.onItemOpened(item.id)
                                                onItemClick(item)
                                            },
                                            modifier = Modifier,
                                            fillWidth = true,
                                            showYear = ListDisplayField.Year in state.displayFields,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        SortByDialog(
            currentSortBy = state.sortBy,
            currentSortOrder = state.sortOrder,
            onUpdate = viewModel::changeSorting,
            onDismissRequest = { showSortDialog = false },
            availableSortOptions = BOOK_SORT_OPTIONS,
        )
    }

    if (showDisplayDialog) {
        DisplayInfoDialog(
            fields = state.displayFields,
            onToggle = viewModel::toggleDisplayField,
            onDismiss = { showDisplayDialog = false },
            availableFields = setOf(ListDisplayField.Year),
        )
    }
}
