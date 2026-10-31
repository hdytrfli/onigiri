package dev.jdtech.jellyfin.presentation.film

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.BooksRoute
import dev.jdtech.jellyfin.LocalTabReselect
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.books.BOOK_SORT_OPTIONS
import dev.jdtech.jellyfin.film.presentation.books.BooksViewModel
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.FindroidBook
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.components.PageSearchField
import dev.jdtech.jellyfin.presentation.components.PageToolbar
import dev.jdtech.jellyfin.presentation.components.RootPage
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.EmptyState
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.OfflineNotice
import dev.jdtech.jellyfin.presentation.film.components.SortByDialog
import dev.jdtech.jellyfin.presentation.film.components.section
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import kotlinx.coroutines.flow.filter

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

    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
        searchQuery = ""
    }

    // Pressing the Books item again cycles Books <-> Audiobooks.
    val tabReselect = LocalTabReselect.current
    LaunchedEffect(tabReselect) {
        tabReselect.events
            .filter { it is BooksRoute }
            .collect { selectedTab = (selectedTab + 1) % 2 }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.loadIfNeeded()
        viewModel.refreshOpenedItem()
        onPauseOrDispose {}
    }

    val audiobooks = selectedTab == 1
    val sectionTitle =
        stringResource(if (audiobooks) CoreR.string.audiobooks_label else CoreR.string.books_label)
    val items =
        state.items.values
            .flatten()
            .filter { item -> item is FindroidBook && item.isAudiobook == audiobooks }
            .distinctBy { it.id }
            .filter { item -> searchQuery.isBlank() || item.name.contains(searchQuery, true) }

    RootPage(
        header = {
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
                            AppIconButton(
                                onClick = { searchExpanded = true },
                                content = {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_search),
                                        contentDescription = stringResource(CoreR.string.search),
                                    )
                                },
                            )
                            AppIconButton(
                                onClick = { showDisplayDialog = true },
                                content = {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_tv_minimal),
                                        contentDescription =
                                            stringResource(CoreR.string.display_info),
                                    )
                                },
                            )
                            AppIconButton(
                                onClick = { showSortDialog = true },
                                content = {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_arrow_down_up),
                                        contentDescription = stringResource(CoreR.string.sort_by),
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
    ) {
        if (LocalOfflineMode.current) {
            OfflineNotice()
            return@RootPage
        }
        if (state.error != null && items.isEmpty()) {
            FullPageError(
                onRefresh = { viewModel.retry() },
                isRefreshing = state.isLoading,
                error = state.error,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && items.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                if (!state.isLoading && items.isEmpty()) {
                    EmptyState(
                        text =
                            stringResource(
                                when {
                                    state.libraries.isEmpty() -> CoreR.string.no_book_libraries
                                    audiobooks -> CoreR.string.no_audiobooks
                                    else -> CoreR.string.no_books
                                }
                            )
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = safePadding.start + MaterialTheme.spacings.gutter,
                            top = LocalRootPageContentInset.current,
                            end = safePadding.end + MaterialTheme.spacings.gutter,
                            bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                    verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                ) {
                    if (items.isNotEmpty()) {
                        section(title = sectionTitle) {
                            items(items = items, key = { it.id }) { item ->
                                ItemCard(
                                    item = item,
                                    direction = Direction.VERTICAL,
                                    onClick = {
                                        viewModel.onItemOpened(item.id)
                                        onItemClick(item)
                                    },
                                    modifier = Modifier.animateItem(),
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
