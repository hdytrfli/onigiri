package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.pulltorefresh.PullToRefreshState
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
import androidx.navigation3.runtime.NavKey
import dev.hdytrfli.onigiri.LocalTabReselect
import dev.hdytrfli.onigiri.MoviesRoute
import dev.hdytrfli.onigiri.ShowsRoute
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionsState
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionsViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.components.Direction
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCard
import dev.hdytrfli.onigiri.feature.film.presentation.components.ListOptionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.MediaSectionRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.section
import dev.hdytrfli.onigiri.feature.film.presentation.library.LibraryAction
import dev.hdytrfli.onigiri.feature.film.presentation.library.LibraryViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.feature.film.presentation.media.GenresState
import dev.hdytrfli.onigiri.feature.film.presentation.media.GenresViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.media.MediaKind
import dev.hdytrfli.onigiri.feature.film.presentation.media.MediaSection
import dev.hdytrfli.onigiri.feature.film.presentation.media.MediaSuggestionsState
import dev.hdytrfli.onigiri.feature.film.presentation.media.MediaSuggestionsViewModel
import dev.hdytrfli.onigiri.models.CollectionType
import dev.hdytrfli.onigiri.models.FindroidGenre
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.UiText
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.PageSearchField
import dev.hdytrfli.onigiri.ui.components.PageToolbar
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.RootPage
import dev.hdytrfli.onigiri.ui.components.TabContent
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

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
    val collectionsViewModel: CollectionsViewModel = hiltViewModel()
    val collectionsState by collectionsViewModel.state.collectAsStateWithLifecycle()
    val genresViewModel: GenresViewModel = hiltViewModel()
    val genresState by genresViewModel.state.collectAsStateWithLifecycle()
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showDisplayDialog by rememberSaveable { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val libraryType =
        if (kind == MediaKind.Movies) CollectionType.Movies else CollectionType.TvShows

    LaunchedEffect(kind) {
        viewModel.load(kind)
        listViewModel.setup(parentId = null, libraryType = libraryType)
        listViewModel.loadItems()
        collectionsViewModel.load(kind)
        genresViewModel.load(kind)
    }

    OfflineModeReload {
        listViewModel.loadItems(force = true)
        viewModel.retry(kind)
        collectionsViewModel.retry(kind)
        genresViewModel.retry(kind)
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

    val activeRefreshing =
        when (selectedTab) {
            0 -> listState.isLoading
            1 -> collectionsState.isLoading
            2 -> state.isLoading
            else -> genresState.isLoading
        }
    val activeRefresh: () -> Unit = {
        when (selectedTab) {
            0 -> listViewModel.onAction(LibraryAction.RefreshItems)
            1 -> collectionsViewModel.retry(kind)
            2 -> viewModel.retry(kind)
            else -> genresViewModel.retry(kind)
        }
    }

    // Search and sort only exist on the list tab; the display-info trigger lives on every
    // tab and drives the one global listDisplayFields setting through the list ViewModel.
    val toolbarActions: @Composable RowScope.() -> Unit = {
        PullToRefreshButton(
            state = pullToRefreshState,
            isRefreshing = activeRefreshing,
            onRefresh = activeRefresh,
        )
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
        ListOptionsMenu(
            onDisplayInfoClick = { showDisplayDialog = true },
            onSortClick = { showSortDialog = true },
            showSort = selectedTab == 0,
        )
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
        TabContent(selectedTab = selectedTab) { tab ->
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
                        pullToRefreshState = pullToRefreshState,
                        showPullIndicator = false,
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
                        displayFields = listState.displayFields,
                        onItemClick = onItemClick,
                        state = collectionsState,
                        onRefresh = { collectionsViewModel.retry(kind) },
                        pullToRefreshState = pullToRefreshState,
                    )
                2 ->
                    MediaSuggestionsContent(
                        state = state,
                        displayFields = listState.displayFields,
                        onItemClick = onItemClick,
                        onListClick = { selectedTab = 0 },
                        onRetry = { viewModel.retry(kind) },
                        pullToRefreshState = pullToRefreshState,
                    )
                else ->
                    GenresContent(
                        displayFields = listState.displayFields,
                        onItemClick = onItemClick,
                        onGenreClick = onGenreClick,
                        state = genresState,
                        onRefresh = { genresViewModel.retry(kind) },
                        pullToRefreshState = pullToRefreshState,
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
    displayFields: Set<ListDisplayField>,
    onItemClick: (FindroidItem) -> Unit,
    state: CollectionsState,
    onRefresh: () -> Unit,
    pullToRefreshState: PullToRefreshState,
) {
    val safePadding = rememberSafePadding()

    val items = state.items
    val title = stringResource(CoreR.string.collections_label)
    val contentPadding =
        PaddingValues(
            start = safePadding.start + MaterialTheme.spacings.gutter,
            top = LocalRootPageContentInset.current,
            end = safePadding.end + MaterialTheme.spacings.gutter,
            bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
        )

    if (state.error != null && items.isEmpty()) {
        FullPageError(
            onRefresh = onRefresh,
            isRefreshing = state.isLoading,
            error = state.error,
        )
        return
    }

    AppPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        showIndicator = false,
    ) {
        EmptyStateOverlay(
            visible = items.isEmpty(),
            loading = state.isLoading,
            context = stringResource(CoreR.string.empty_context_collections),
            onRefresh = onRefresh,
            title = title,
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
    pullToRefreshState: PullToRefreshState,
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
        state = pullToRefreshState,
        showIndicator = false,
    ) {
        if (state.error != null && state.sections.isEmpty()) {
            FullPageError(
                onRefresh = onRetry,
                isRefreshing = state.isLoading,
                error = state.error,
            )
        } else {
            // Keep a scrollable container behind the overlay so pull-to-refresh still works when
            // there are no rows.
            EmptyStateOverlay(
                visible = state.sections.isEmpty(),
                loading = state.isLoading,
                context = stringResource(CoreR.string.empty_context_suggestions),
                onRefresh = onRetry,
                title = stringResource(FilmR.string.suggestions),
                titlePadding = PaddingValues(horizontal = MaterialTheme.spacings.gutter),
                contentPadding = contentPadding,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                ) {
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
    displayFields: Set<ListDisplayField>,
    onItemClick: (FindroidItem) -> Unit,
    onGenreClick: (FindroidGenre) -> Unit,
    state: GenresState,
    onRefresh: () -> Unit,
    pullToRefreshState: PullToRefreshState,
) {
    val safePadding = rememberSafePadding()

    AppPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        showIndicator = false,
    ) {
        if (state.error != null && state.sections.isEmpty()) {
            FullPageError(
                onRefresh = onRefresh,
                isRefreshing = state.isLoading,
                error = state.error,
            )
        } else {
            val contentPadding =
                PaddingValues(
                    top = LocalRootPageContentInset.current,
                    bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
                )
            EmptyStateOverlay(
                visible = state.sections.isEmpty(),
                loading = state.isLoading,
                context = stringResource(CoreR.string.empty_context_genres),
                onRefresh = onRefresh,
                title = stringResource(FilmR.string.genres),
                titlePadding = PaddingValues(horizontal = MaterialTheme.spacings.gutter),
                contentPadding = contentPadding,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                ) {
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
                            onListClick = { onGenreClick(FindroidGenre(section.id, section.name)) },
                        )
                    }
                }
            }
        }
    }
}
