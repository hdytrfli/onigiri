package dev.jdtech.jellyfin.presentation.film

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
import dev.jdtech.jellyfin.HomeRoute
import dev.jdtech.jellyfin.LocalTabReselect
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeSection
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeSuggestions
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeView
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServer
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.downloads.DownloadsViewModel
import dev.jdtech.jellyfin.film.presentation.favorites.FavoritesViewModel
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.film.presentation.home.HomeState
import dev.jdtech.jellyfin.film.presentation.home.HomeViewModel
import dev.jdtech.jellyfin.film.presentation.media.MediaSection
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.AppPullToRefreshBox
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.components.PageToolbar
import dev.jdtech.jellyfin.presentation.components.RootPage
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.HomeCarousel
import dev.jdtech.jellyfin.presentation.film.components.HomeSection
import dev.jdtech.jellyfin.presentation.film.components.HomeView
import dev.jdtech.jellyfin.presentation.film.components.MediaSectionRow
import dev.jdtech.jellyfin.presentation.film.components.OfflineNotice
import dev.jdtech.jellyfin.presentation.film.components.Section
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import kotlinx.coroutines.flow.filter

@Composable
fun HomeScreen(
    onLibraryClick: (library: FindroidCollection) -> Unit,
    onBooksClick: (audiobooks: Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (item: FindroidItem) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Favorites and Downloads render inside this page but their display options are edited from
    // the shared toolbar, so their ViewModels are hoisted here and handed down.
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favoritesState by favoritesViewModel.state.collectAsStateWithLifecycle()
    val downloadsViewModel: DownloadsViewModel = hiltViewModel()
    val downloadsState by downloadsViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.refresh() }

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showDisplayDialog by rememberSaveable { mutableStateOf(false) }

    // Pressing the Home item again cycles Home -> Favorites -> Downloads.
    val tabReselect = LocalTabReselect.current
    LaunchedEffect(tabReselect) {
        tabReselect.events
            .filter { it is HomeRoute }
            .collect { selectedTab = (selectedTab + 1) % 3 }
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
                    AppIconButton(
                        onClick = { showDisplayDialog = true },
                        variant = AppButtonVariant.Secondary,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_tv_minimal),
                                contentDescription = null,
                            )
                        },
                    )
                },
            )
        }
    ) {
        when (selectedTab) {
            0 ->
                HomeScreenLayout(
                    state = state,
                    onAction = onAction,
                    onBooksClick = onBooksClick,
                )
            1 ->
                FavoritesScreen(
                    onItemClick = onItemClick,
                    viewModel = favoritesViewModel,
                )
            else ->
                DownloadsScreen(
                    onItemClick = onItemClick,
                    embedded = true,
                    viewModel = downloadsViewModel,
                )
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
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter

    val itemsPadding = PaddingValues(start = paddingStart, end = paddingEnd)

    val contentPaddingTop = LocalRootPageContentInset.current

    if (LocalOfflineMode.current) {
        OfflineNotice()
        return
    }

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
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().semantics { traversalIndex = 1f },
                contentPadding = PaddingValues(top = contentPaddingTop, bottom = paddingBottom),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                state.suggestionsSection?.let { section ->
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
