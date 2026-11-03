package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.HomeRoute
import dev.hdytrfli.onigiri.LocalTabReselect
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyState
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.HomeCarousel
import dev.hdytrfli.onigiri.feature.film.presentation.components.HomeSection
import dev.hdytrfli.onigiri.feature.film.presentation.components.HomeView
import dev.hdytrfli.onigiri.feature.film.presentation.components.ListOptionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.MediaSectionRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.Section
import dev.hdytrfli.onigiri.feature.film.presentation.downloads.DownloadsViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.favorites.FavoritesViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeAction
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeSearchContent
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeSearchResult
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeSearchViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeState
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.media.MediaSection
import dev.hdytrfli.onigiri.models.FindroidCollection
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.PageSearchField
import dev.hdytrfli.onigiri.ui.components.PageToolbar
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.RootPage
import dev.hdytrfli.onigiri.ui.components.TabContent
import dev.hdytrfli.onigiri.ui.preview.dummyHomeSection
import dev.hdytrfli.onigiri.ui.preview.dummyHomeSuggestions
import dev.hdytrfli.onigiri.ui.preview.dummyHomeView
import dev.hdytrfli.onigiri.ui.preview.dummyServer
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

@Composable
fun HomeScreen(
    onLibraryClick: (library: FindroidCollection) -> Unit,
    onBooksClick: (audiobooks: Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (item: FindroidItem) -> Unit,
    onSearchResultClick: (HomeSearchResult) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: HomeSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()
    // Favorites and Downloads render inside this page but their display options are edited from
    // the shared toolbar, so their ViewModels are hoisted here and handed down.
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favoritesState by favoritesViewModel.state.collectAsStateWithLifecycle()
    val downloadsViewModel: DownloadsViewModel = hiltViewModel()
    val downloadsState by downloadsViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.refresh() }
    OfflineModeReload { viewModel.loadData(force = true) }

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showDisplayDialog by rememberSaveable { mutableStateOf(false) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val safePadding = rememberSafePadding()
    val pullToRefreshState = rememberPullToRefreshState()
    val closeSearch: () -> Unit = {
        searchExpanded = false
        searchQuery = ""
        searchViewModel.search("")
    }

    BackHandler(enabled = searchExpanded) { closeSearch() }
    LaunchedEffect(searchQuery) { searchViewModel.search(searchQuery) }
    val activeRefreshing =
        when (selectedTab) {
            0 -> state.isLoading
            1 -> favoritesState.isLoading
            else -> downloadsState.isLoading
        }
    val activeRefresh: () -> Unit = {
        when (selectedTab) {
            0 -> viewModel.onAction(HomeAction.OnRetryClick)
            1 -> favoritesViewModel.loadItems(force = true)
            else -> downloadsViewModel.loadItems(force = true)
        }
    }

    // Pressing the Home item again cycles Home -> Favorites -> Downloads.
    val tabReselect = LocalTabReselect.current
    LaunchedEffect(tabReselect) {
        tabReselect.reselects(HomeRoute).collect { selectedTab = (selectedTab + 1) % 3 }
    }

    // The offline notice's Downloads action: open this page on the Downloads tab.
    val openDownloads by tabReselect.openDownloads.collectAsState()
    LaunchedEffect(openDownloads) {
        if (openDownloads) {
            selectedTab = 2
            tabReselect.consumeDownloads()
        }
    }

    val onAction: (HomeAction) -> Unit = { action ->
        when (action) {
            is HomeAction.OnItemClick -> onItemClick(action.item)
            is HomeAction.OnLibraryClick -> onLibraryClick(action.library)
            is HomeAction.OnSearchClick -> onSearchClick()
            is HomeAction.OnSettingsClick -> onSettingsClick()
            else -> Unit
        }
        viewModel.onAction(action)
    }

    RootPage(
        header = {
            AnimatedContent(targetState = searchExpanded, label = "home-search") { expanded ->
                if (expanded) {
                    PageSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        onClose = closeSearch,
                    )
                } else {
                    PageToolbar(
                        tabs =
                            listOf(
                                stringResource(CoreR.string.title_home),
                                stringResource(CoreR.string.title_favorite),
                                stringResource(CoreR.string.title_download),
                            ),
                        tabIcons =
                            listOf(
                                CoreR.drawable.ic_home,
                                CoreR.drawable.ic_heart,
                                CoreR.drawable.ic_download,
                            ),
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it },
                        dropdownOnCompact = true,
                        actions = {
                            PullToRefreshButton(
                                state = pullToRefreshState,
                                isRefreshing = activeRefreshing,
                                onRefresh = activeRefresh,
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
                                onSortClick = {},
                                showSort = false,
                            )
                        },
                    )
                }
            }
        }
    ) {
        if (searchExpanded) {
            HomeSearchContent(
                state = searchState,
                contentPadding =
                    PaddingValues(
                        start = safePadding.start + MaterialTheme.spacings.gutter,
                        top = LocalRootPageContentInset.current,
                        end = safePadding.end + MaterialTheme.spacings.gutter,
                        bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
                    ),
                onResultClick = onSearchResultClick,
                onRetry = searchViewModel::retry,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            TabContent(selectedTab = selectedTab) { tab ->
                when (tab) {
                    0 ->
                        HomeScreenLayout(
                            state = state,
                            onAction = onAction,
                            onBooksClick = onBooksClick,
                            pullToRefreshState = pullToRefreshState,
                        )
                    1 ->
                        FavoritesScreen(
                            onItemClick = onItemClick,
                            viewModel = favoritesViewModel,
                            pullToRefreshState = pullToRefreshState,
                            showPullIndicator = false,
                        )
                    else ->
                        DownloadsScreen(
                            onItemClick = onItemClick,
                            embedded = true,
                            viewModel = downloadsViewModel,
                            pullToRefreshState = pullToRefreshState,
                            showPullIndicator = false,
                        )
                }
            }
        }
    }

    // The dialog edits the fields of the tab it was opened from; the other pages pick the shared
    // preference up when they next load.
    if (showDisplayDialog) {
        val onDismiss = { showDisplayDialog = false }
        when (selectedTab) {
            0 ->
                DisplayInfoDialog(
                    fields = state.displayFields,
                    onToggle = { viewModel.onAction(HomeAction.ToggleDisplayField(it)) },
                    onDismiss = onDismiss,
                )
            1 ->
                DisplayInfoDialog(
                    fields = favoritesState.displayFields,
                    onToggle = { favoritesViewModel.toggleDisplayField(it) },
                    onDismiss = onDismiss,
                )
            else ->
                DisplayInfoDialog(
                    fields = downloadsState.displayFields,
                    onToggle = { downloadsViewModel.toggleDisplayField(it) },
                    onDismiss = onDismiss,
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenLayout(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    onBooksClick: (audiobooks: Boolean) -> Unit = {},
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter

    val itemsPadding = PaddingValues(start = paddingStart, end = paddingEnd)

    val contentPaddingTop = LocalRootPageContentInset.current
    val offlineMode = LocalOfflineMode.current
    val hasContent =
        state.resumeSection != null ||
            state.nextUpSection != null ||
            state.latestBooksSection != null ||
            state.latestAudiobooksSection != null ||
            state.views.isNotEmpty() ||
            (!offlineMode && state.suggestionsSection != null)

    if (state.error != null && state.suggestionsSection == null && state.views.isEmpty()) {
        FullPageError(
            onRefresh = { onAction(HomeAction.OnRetryClick) },
            isRefreshing = state.isLoading,
            error = state.error,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().semantics { isTraversalGroup = true }) {
        AppPullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { onAction(HomeAction.OnRetryClick) },
            state = pullToRefreshState,
            showIndicator = false,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().semantics { traversalIndex = 1f },
                contentPadding = PaddingValues(top = contentPaddingTop, bottom = paddingBottom),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                if (!hasContent && !state.isLoading) {
                    item(key = "empty-state") {
                        EmptyState(
                            context = stringResource(CoreR.string.empty_context_media),
                            onRefresh = { onAction(HomeAction.OnRetryClick) },
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                }
                state.suggestionsSection
                    ?.takeUnless { offlineMode }
                    ?.let { section ->
                        item(key = section.id) {
                            Section(
                                title = stringResource(FilmR.string.suggestions),
                                titleModifier =
                                    Modifier.padding(start = paddingStart, end = paddingEnd),
                            ) {
                                HomeCarousel(
                                    items = section.items,
                                    itemsPadding = itemsPadding,
                                    onAction = onAction,
                                )
                            }
                        }
                    }
                state.resumeSection?.let { section ->
                    item(key = section.id) {
                        MediaSectionRow(
                            section =
                                MediaSection(
                                    id = section.id.toString(),
                                    title = section.homeSection.name,
                                    items = section.homeSection.items,
                                    useThumbnails = true,
                                    showRemainingTime = true,
                                ),
                            displayFields = state.displayFields,
                            onItemClick = { item -> onAction(HomeAction.OnItemClick(item)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                state.nextUpSection?.let { section ->
                    item(key = section.id) {
                        HomeSection(
                            section = section.homeSection,
                            itemsPadding = itemsPadding,
                            onAction = onAction,
                            modifier = Modifier.animateItem(),
                            displayFields = state.displayFields,
                        )
                    }
                }
                items(state.views, key = { it.id }) { view ->
                    HomeView(
                        view = view,
                        itemsPadding = itemsPadding,
                        onAction = onAction,
                        modifier = Modifier.animateItem(),
                        displayFields = state.displayFields,
                    )
                }
                state.latestBooksSection?.let { section ->
                    item(key = section.id) {
                        HomeSection(
                            section = section.homeSection,
                            itemsPadding = itemsPadding,
                            onAction = onAction,
                            modifier = Modifier.animateItem(),
                            displayFields = state.displayFields,
                            gridSize = GridSize.POSTER,
                            vertical = true,
                            onMoreClick = { onBooksClick(false) },
                        )
                    }
                }
                state.latestAudiobooksSection?.let { section ->
                    item(key = section.id) {
                        HomeSection(
                            section = section.homeSection,
                            itemsPadding = itemsPadding,
                            onAction = onAction,
                            modifier = Modifier.animateItem(),
                            displayFields = state.displayFields,
                            gridSize = GridSize.POSTER,
                            vertical = true,
                            onMoreClick = { onBooksClick(true) },
                        )
                    }
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun HomeScreenLayoutPreview() {
    FindroidTheme {
        HomeScreenLayout(
            state =
                HomeState(
                    server = dummyServer,
                    suggestionsSection = dummyHomeSuggestions,
                    resumeSection = dummyHomeSection,
                    views = listOf(dummyHomeView),
                    error = Exception("Failed to load data"),
                ),
            onAction = {},
        )
    }
}
