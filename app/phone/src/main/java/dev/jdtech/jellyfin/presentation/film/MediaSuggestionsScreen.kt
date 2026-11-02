package dev.jdtech.jellyfin.presentation.film

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation3.runtime.NavKey
import dev.jdtech.jellyfin.LocalTabReselect
import dev.jdtech.jellyfin.MoviesRoute
import dev.jdtech.jellyfin.ShowsRoute
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.collection.CollectionsViewModel
import dev.jdtech.jellyfin.film.presentation.library.LibraryAction
import dev.jdtech.jellyfin.film.presentation.library.LibraryViewModel
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.film.presentation.media.GenresViewModel
import dev.jdtech.jellyfin.film.presentation.media.MediaKind
import dev.jdtech.jellyfin.film.presentation.media.MediaSection
import dev.jdtech.jellyfin.film.presentation.media.MediaSuggestionsState
import dev.jdtech.jellyfin.film.presentation.media.MediaSuggestionsViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidGenre
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.components.AnimatedTabContent
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.AppPullToRefreshBox
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.components.OfflineModeReload
import dev.jdtech.jellyfin.presentation.components.PageSearchField
import dev.jdtech.jellyfin.presentation.components.PageToolbar
import dev.jdtech.jellyfin.presentation.components.RootPage
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.EmptyState
import dev.jdtech.jellyfin.presentation.film.components.EmptyStateOverlay
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.MediaSectionRow
import dev.jdtech.jellyfin.presentation.film.components.section
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

/** The Movies root page: Movies (sortable list), Suggestions and Genres tabs. */
@Composable
fun MoviesScreen(
    onItemClick: (FindroidItem) -> Unit,
    onGenreClick: (FindroidGenre) -> Unit,
    viewModel: MediaSuggestionsViewModel = hiltViewModel(),
) {
    MediaPage(MediaKind.Movies, onItemClick, onGenreClick, viewModel)
}

/** The Shows root page: Shows (sortable list), Suggestions and Genres tabs. */
@Composable
fun ShowsScreen(
    onItemClick: (FindroidItem) -> Unit,
    onGenreClick: (FindroidGenre) -> Unit,
    viewModel: MediaSuggestionsViewModel = hiltViewModel(),
) {
    MediaPage(MediaKind.Shows, onItemClick, onGenreClick, viewModel)
}

@Composable
private fun MediaPage(
    kind: MediaKind,
    onItemClick: (FindroidItem) -> Unit,
    onGenreClick: (FindroidGenre) -> Unit,
    viewModel: MediaSuggestionsViewModel,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val listViewModel: LibraryViewModel = hiltViewModel()
    val listState by listViewModel.state.collectAsStateWithLifecycle()
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showDisplayDialog by rememberSaveable { mutableStateOf(false) }
    val libraryType =
        if (kind == MediaKind.Movies) CollectionType.Movies else CollectionType.TvShows

    LaunchedEffect(kind) {
        viewModel.load(kind)
        listViewModel.setup(parentId = null, libraryType = libraryType)
        listViewModel.loadItems()
    }

    OfflineModeReload {
        listViewModel.loadItems(force = true)
        viewModel.retry(kind)
    }

    // Re-tap cycles the pills, exactly like Home: list -> collections -> suggestions -> genres.
    val tabReselect = LocalTabReselect.current
    LaunchedEffect(tabReselect, kind) {
        val route: NavKey = if (kind == MediaKind.Movies) MoviesRoute else ShowsRoute
        tabReselect.reselects(route).collect { selectedTab = (selectedTab + 1) % 4 }
    }

    // Back closes the search instead of leaving the page: the query is cleared on the way out so
    // reopening starts fresh.
    BackHandler(enabled = listState.searchExpanded) {
        listViewModel.onAction(LibraryAction.SetSearchQuery(""))
        listViewModel.onAction(LibraryAction.ToggleSearch)
    }

    // Search and sort only exist on the list tab; the display-info trigger lives on every
    // tab and drives the one global listDisplayFields setting through the list ViewModel.
    val toolbarActions: @Composable RowScope.() -> Unit = {
        if (selectedTab == 0 && !LocalOfflineMode.current) {
            AppIconButton(
                onClick = { listViewModel.onAction(LibraryAction.ToggleSearch) },
                variant = AppButtonVariant.Secondary,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_search),
                        contentDescription = stringResource(CoreR.string.search),
                    )
                },
            )
        }
        AppIconButton(
            onClick = { showDisplayDialog = true },
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_tv_minimal),
                    contentDescription = stringResource(CoreR.string.display_info),
                )
            },
        )
        if (selectedTab == 0) {
            AppIconButton(
                onClick = { showSortDialog = true },
                variant = AppButtonVariant.Secondary,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_arrow_down_up),
                        contentDescription = stringResource(CoreR.string.sort_by),
                    )
                },
            )
        }
    }

    RootPage(
        header = {
            // The search icon expands into the field in place; its x collapses it back.
            AnimatedContent(
                targetState = listState.searchExpanded,
                label = "media-search",
            ) { searchExpanded ->
                if (searchExpanded) {
                    PageSearchField(
                        value = listState.searchQuery,
                        onValueChange = {
                            listViewModel.onAction(LibraryAction.SetSearchQuery(it))
                        },
                        onClose = { listViewModel.onAction(LibraryAction.ToggleSearch) },
                    )
                } else {
                    PageToolbar(
                        tabs =
                            listOf(
                                stringResource(
                                    if (kind == MediaKind.Movies) CoreR.string.movies_label
                                    else CoreR.string.shows_label
                                ),
                                stringResource(CoreR.string.collections_label),
                                stringResource(FilmR.string.suggestions),
                                stringResource(FilmR.string.genres),
                            ),
                        tabIcons =
                            listOf(
                                if (kind == MediaKind.Movies) CoreR.drawable.ic_clapperboard
                                else CoreR.drawable.ic_tv,
                                CoreR.drawable.ic_package_2,
                                CoreR.drawable.ic_star,
                                CoreR.drawable.ic_layout_grid,
                            ),
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it },
                        dropdownOnCompact = true,
                        actions = toolbarActions,
                    )
                }
            }
        }
    ) {
        AnimatedTabContent(selectedTab = selectedTab) { tab ->
            when (tab) {
                0 ->
                    LibraryContent(
                        state = listState,
                        onAction = { action ->
                            if (action is LibraryAction.OnItemClick) {
                                onItemClick(action.item)
                            }
                            listViewModel.onAction(action)
                        },
                        showSortDialog = showSortDialog,
                        showDisplayDialog = false,
                        onShowSortDialogChange = { showSortDialog = it },
                        onShowDisplayDialogChange = {},
                        embedded = true,
                        sectionTitle =
                            stringResource(
                                if (kind == MediaKind.Movies) CoreR.string.all_movies
                                else CoreR.string.all_shows
                            ),
                        showSearchField = false,
                    )
                1 ->
                    CollectionsTab(
                        kind = kind,
                        displayFields = listState.displayFields,
                        onItemClick = onItemClick,
                    )
                2 ->
                    MediaSuggestionsContent(
                        state = state,
                        displayFields = listState.displayFields,
                        onItemClick = onItemClick,
                        onListClick = { selectedTab = 0 },
                        onRetry = { viewModel.retry(kind) },
                    )
                else ->
                    GenresContent(
                        kind = kind,
                        displayFields = listState.displayFields,
                        onItemClick = onItemClick,
                        onGenreClick = onGenreClick,
                    )
            }
        }
        // One dialog for every tab; the setting itself is global, never stored per page.
        if (showDisplayDialog) {
            DisplayInfoDialog(
                fields = listState.displayFields,
                onToggle = { listViewModel.onAction(LibraryAction.ToggleDisplayField(it)) },
                onDismiss = { showDisplayDialog = false },
            )
        }
    }
}

@Composable
private fun CollectionsTab(
    kind: MediaKind,
    displayFields: Set<ListDisplayField>,
    onItemClick: (FindroidItem) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val safePadding = rememberSafePadding()

    LaunchedEffect(kind) { viewModel.load() }
    OfflineModeReload { viewModel.retry() }

    val items = if (kind == MediaKind.Movies) state.movies else state.shows
    val title = stringResource(CoreR.string.collections_label)

    if (state.error != null && items.isEmpty()) {
        FullPageError(
            onRefresh = { viewModel.retry() },
            isRefreshing = state.isLoading,
            error = state.error,
        )
        return
    }

    AppPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = viewModel::retry,
        modifier = Modifier.fillMaxSize(),
    ) {
        EmptyStateOverlay(
            visible = !state.isLoading && items.isEmpty(),
            context = stringResource(CoreR.string.empty_context_collections),
            onRefresh = { viewModel.retry() },
        ) {
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
                    section(title = title) {
                        gridItems(items = items, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                direction = Direction.VERTICAL,
                                onClick = { onItemClick(item) },
                                modifier = Modifier.animateItem(),
                                fillWidth = true,
                                showYear = ListDisplayField.Year in displayFields,
                                showCommunityRating =
                                    ListDisplayField.CommunityRating in displayFields,
                                showAgeRating = ListDisplayField.AgeRating in displayFields,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaSuggestionsContent(
    state: MediaSuggestionsState,
    displayFields: Set<ListDisplayField>,
    onItemClick: (FindroidItem) -> Unit,
    onListClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val safePadding = rememberSafePadding()
    val contentPadding =
        PaddingValues(
            top = LocalRootPageContentInset.current,
            bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
        )

    OfflineModeReload { onRetry() }

    AppPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRetry,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.error != null && state.sections.isEmpty()) {
            FullPageError(
                onRefresh = onRetry,
                isRefreshing = state.isLoading,
                error = state.error,
            )
        } else {
            // Keep a scrollable container even when there are no rows, otherwise the pull gesture
            // has nothing to attach to and the empty state cannot be refreshed by dragging.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                if (state.sections.isEmpty()) {
                    item {
                        EmptyState(
                            context = stringResource(CoreR.string.empty_context_suggestions),
                            onRefresh = onRetry,
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                } else {
                    items(items = state.sections, key = { it.id }) { section ->
                        MediaSectionRow(
                            section = section,
                            displayFields = displayFields,
                            onItemClick = onItemClick,
                            onListClick = if (section.showsListLink) onListClick else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenresContent(
    kind: MediaKind,
    displayFields: Set<ListDisplayField>,
    onItemClick: (FindroidItem) -> Unit,
    onGenreClick: (FindroidGenre) -> Unit,
    viewModel: GenresViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val safePadding = rememberSafePadding()

    // Load once; the cached sections stay put until the user pulls to refresh or the cache is
    // invalidated by a new ViewModel.
    LaunchedEffect(kind) { viewModel.load(kind) }
    OfflineModeReload { viewModel.retry(kind) }

    AppPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.retry(kind) },
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.error != null && state.sections.isEmpty()) {
                FullPageError(
                    onRefresh = { viewModel.retry(kind) },
                    isRefreshing = state.isLoading,
                    error = state.error,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = LocalRootPageContentInset.current,
                            bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
                        ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                ) {
                    if (state.sections.isEmpty()) {
                        item {
                            EmptyState(
                                context = stringResource(CoreR.string.empty_context_genres),
                                onRefresh = { viewModel.retry(kind) },
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    } else {
                        items(items = state.sections, key = { it.id }) { section ->
                            MediaSectionRow(
                                section =
                                    MediaSection(
                                        id = section.id.toString(),
                                        title = UiText.DynamicString(section.name),
                                        items = section.items,
                                    ),
                                displayFields = displayFields,
                                onItemClick = onItemClick,
                                onListClick = {
                                    onGenreClick(FindroidGenre(section.id, section.name))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
