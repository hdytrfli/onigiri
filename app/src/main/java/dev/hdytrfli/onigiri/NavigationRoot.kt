package dev.hdytrfli.onigiri

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.BookDetailScreen
import dev.hdytrfli.onigiri.feature.film.presentation.BooksScreen
import dev.hdytrfli.onigiri.feature.film.presentation.CollectionScreen
import dev.hdytrfli.onigiri.feature.film.presentation.DownloadsScreen
import dev.hdytrfli.onigiri.feature.film.presentation.EditImageSearchScreen
import dev.hdytrfli.onigiri.feature.film.presentation.EditImagesScreen
import dev.hdytrfli.onigiri.feature.film.presentation.EpisodeScreen
import dev.hdytrfli.onigiri.feature.film.presentation.GenreScreen
import dev.hdytrfli.onigiri.feature.film.presentation.HomeScreen
import dev.hdytrfli.onigiri.feature.film.presentation.IdentifyResultsScreen
import dev.hdytrfli.onigiri.feature.film.presentation.IdentifyScreen
import dev.hdytrfli.onigiri.feature.film.presentation.LibraryScreen
import dev.hdytrfli.onigiri.feature.film.presentation.MediaScreen
import dev.hdytrfli.onigiri.feature.film.presentation.MovieScreen
import dev.hdytrfli.onigiri.feature.film.presentation.MoviesScreen
import dev.hdytrfli.onigiri.feature.film.presentation.PersonScreen
import dev.hdytrfli.onigiri.feature.film.presentation.SeasonScreen
import dev.hdytrfli.onigiri.feature.film.presentation.ShowScreen
import dev.hdytrfli.onigiri.feature.film.presentation.ShowsScreen
import dev.hdytrfli.onigiri.feature.film.presentation.SubtitleSearchScreen
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeSearchResult
import dev.hdytrfli.onigiri.feature.film.presentation.music.AlbumDetailScreen
import dev.hdytrfli.onigiri.feature.film.presentation.music.ArtistDetailScreen
import dev.hdytrfli.onigiri.feature.film.presentation.music.MusicScreen
import dev.hdytrfli.onigiri.models.CollectionType
import dev.hdytrfli.onigiri.models.FindroidBook
import dev.hdytrfli.onigiri.models.FindroidBoxSet
import dev.hdytrfli.onigiri.models.FindroidCollection
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidFolder
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidSeason
import dev.hdytrfli.onigiri.models.FindroidShow
import dev.hdytrfli.onigiri.navigation.navigateOrReuse
import dev.hdytrfli.onigiri.navigation.resumedAction
import dev.hdytrfli.onigiri.presentation.onboarding.AddProfileScreen
import dev.hdytrfli.onigiri.presentation.onboarding.OnboardingScreen
import dev.hdytrfli.onigiri.presentation.player.MiniMusicPlayerHost
import dev.hdytrfli.onigiri.presentation.player.MusicPlayerViewModel
import dev.hdytrfli.onigiri.presentation.reader.ReaderFontScreen
import dev.hdytrfli.onigiri.presentation.settings.AboutScreen
import dev.hdytrfli.onigiri.presentation.settings.ProfilePickerScreen
import dev.hdytrfli.onigiri.presentation.settings.SettingsFileEditScreen
import dev.hdytrfli.onigiri.presentation.settings.SettingsProfileHeader
import dev.hdytrfli.onigiri.presentation.settings.SettingsScreen
import dev.hdytrfli.onigiri.presentation.settings.SubtitleFontScreen
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.swr.SwrStore
import dev.hdytrfli.onigiri.ui.components.AppSnackbarHost
import dev.hdytrfli.onigiri.ui.components.AppSnackbarTone
import dev.hdytrfli.onigiri.ui.components.TAB_TRANSITION_SPEC
import dev.hdytrfli.onigiri.ui.components.showAppSnackbar
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.LocalPlaybackLauncher
import dev.hdytrfli.onigiri.ui.utils.LocalPredictiveBack
import dev.hdytrfli.onigiri.ui.utils.PlaybackLauncher
import java.util.UUID
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType

private val SETTINGS_ROOT_ROUTE = SettingsRoute(indexes = emptyList())

private val TOP_LEVEL_ROUTES =
    setOf(HomeRoute, MoviesRoute, ShowsRoute, BooksRoute, MusicRoute, SETTINGS_ROOT_ROUTE)

data class TabBarItem(
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val route: NavKey,
    val enabled: Boolean = true,
)

val homeTab =
    TabBarItem(title = CoreR.string.title_home, icon = CoreR.drawable.ic_home, route = HomeRoute)

val moviesTab =
    TabBarItem(
        title = CoreR.string.movies_label,
        icon = CoreR.drawable.ic_clapperboard,
        route = MoviesRoute,
    )

val showsTab =
    TabBarItem(
        title = CoreR.string.shows_label,
        icon = CoreR.drawable.ic_tv,
        route = ShowsRoute,
    )

val booksTab =
    TabBarItem(
        title = CoreR.string.books_label,
        icon = CoreR.drawable.ic_book_open,
        route = BooksRoute,
    )

val musicTab =
    TabBarItem(
        title = CoreR.string.title_music,
        icon = CoreR.drawable.ic_music,
        route = MusicRoute,
    )

val settingsTab =
    TabBarItem(
        title = CoreR.string.title_settings,
        icon = CoreR.drawable.ic_user,
        route = SETTINGS_ROOT_ROUTE,
    )

@Composable
fun NavigationRoot(
    hasServers: Boolean,
    hasCurrentServer: Boolean,
    hasCurrentUser: Boolean,
    onboardingComplete: Boolean,
    profileLoggedOut: Boolean,
    swrStore: SwrStore,
    confirmExit: Boolean = false,
) {
    val setupComplete = onboardingComplete && hasServers && hasCurrentServer && hasCurrentUser
    val loggedOutProfileSelection =
        onboardingComplete && hasCurrentServer && !hasCurrentUser && profileLoggedOut
    val mainMode = setupComplete || loggedOutProfileSelection

    val setupStartRoute: NavKey = OnboardingRoute

    // Navigation state for both modes is hoisted so that it survives switching between them.
    val setupBackStack = rememberNavBackStack(setupStartRoute)
    val mainStartRoute: NavKey =
        if (loggedOutProfileSelection) ProfilePickerRoute(canGoBack = false) else HomeRoute
    val mainTopLevelRoutes: Set<NavKey> =
        if (loggedOutProfileSelection) TOP_LEVEL_ROUTES + mainStartRoute else TOP_LEVEL_ROUTES
    val navigationState =
        rememberNavigationState(
            startRoute = mainStartRoute,
            topLevelRoutes = mainTopLevelRoutes,
        )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    // The entry decorators hold the per-entry SavedState and ViewModel stores. They must be
    // hoisted as well, otherwise the retained state would be discarded when switching modes.
    val saveableStateHolderDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
    // Every scene needs its own opaque base. Without it, a predictive-back preview can show the
    // previous page through screens that use a transparent root.
    val pageBackground = MaterialTheme.colorScheme.background
    val pageBackgroundDecorator =
        remember(pageBackground) {
            NavEntryDecorator<NavKey> { entry ->
                Box(Modifier.fillMaxSize().background(pageBackground)) { entry.Content() }
            }
        }
    val entryDecorators =
        remember(saveableStateHolderDecorator, viewModelStoreDecorator, pageBackgroundDecorator) {
            listOf(saveableStateHolderDecorator, viewModelStoreDecorator, pageBackgroundDecorator)
        }

    // Exposed as a state object (rather than a plain value) so the entry content closures, which
    // nav3 caches per back stack snapshot, always read the current value.
    val searchExpanded = remember { mutableStateOf(false) }

    // Keep the back stacks consistent across mode switches. This is an effect rather than a
    // composition-time mutation; the navigation state is already retained per mode, so the reset
    // only normalizes the stacks that the new mode is about to show.
    LaunchedEffect(mainMode, loggedOutProfileSelection) {
        if (mainMode) {
            if (!loggedOutProfileSelection) navigator.navigateHome()
        } else {
            setupBackStack.clear()
            setupBackStack.add(setupStartRoute)
        }
    }

    if (mainMode) {
        MainNavigation(
            navigationState = navigationState,
            navigator = navigator,
            entryDecorators = entryDecorators,
            isOfflineMode = LocalOfflineMode.current,
            searchExpanded = searchExpanded,
            confirmExit = confirmExit,
            swrStore = swrStore,
        )
    } else {
        SetupNavigation(
            backStack = setupBackStack,
            entryDecorators = entryDecorators,
        )
    }
}

@Composable
private fun MainNavigation(
    navigationState: NavigationState,
    navigator: Navigator,
    entryDecorators: List<NavEntryDecorator<NavKey>>,
    isOfflineMode: Boolean,
    searchExpanded: MutableState<Boolean>,
    confirmExit: Boolean,
    swrStore: SwrStore,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val currentMusic by musicPlayerViewModel.current.collectAsStateWithLifecycle()
    var exitPending by remember { mutableStateOf(false) }

    LaunchedEffect(swrStore) {
        swrStore.failures.collect { failure ->
            val messageRes =
                when {
                    failure.isOffline && failure.hasData -> R.string.swr_offline_cached
                    failure.isOffline -> R.string.swr_offline_no_data
                    else -> R.string.swr_refresh_failed
                }
            snackbarHostState.showAppSnackbar(
                message = context.getString(messageRes),
                duration = SnackbarDuration.Short,
                tone =
                    if (failure.isOffline) {
                        AppSnackbarTone.Offline
                    } else {
                        AppSnackbarTone.Error
                    },
            )
        }
    }

    val tabRoutes =
        listOf(homeTab, moviesTab, showsTab, booksTab, musicTab, settingsTab).map { it.route }

    val topLevelRoute = navigationState.topLevelRoute

    // Press back twice to exit from the start route's root.
    val atExitRoot =
        navigationState.currentBackStack().size <= 1 &&
            navigationState.topLevelRoute == navigationState.startRoute
    BackHandler(enabled = confirmExit && atExitRoot) {
        if (exitPending) {
            (context as? Activity)?.finish()
        } else {
            exitPending = true
            scope.launch {
                snackbarHostState.showAppSnackbar(
                    message = context.getString(SettingsR.string.back_again_to_close),
                    duration = SnackbarDuration.Short,
                )
                exitPending = false
            }
        }
    }

    // The search bar belongs to the Media tab: collapse it when the active top level route
    // changes away from Media (e.g. via the home button on a detail screen) so that the bottom
    // bar is shown again.
    LaunchedEffect(topLevelRoute) {
        if (topLevelRoute != MediaRoute) {
            searchExpanded.value = false
        }
    }

    val tabReselect = remember { TabReselect() }

    val entryProvider = entryProvider {
        mainEntries(
            navigator = navigator,
            tabReselect = tabReselect,
            onSearchClick = {
                searchExpanded.value = true
                navigator.navigate(MediaRoute)
            },
            searchExpanded = searchExpanded,
        )
    }

    val suiteType = rememberNavigationSuiteType()
    val predictiveBack = LocalPredictiveBack.current
    val currentRoute = navigationState.currentBackStack().last()
    CompositionLocalProvider(
        LocalSuiteStartInset provides suiteType.suiteStartInset(),
        LocalSuiteBottomInset provides suiteType.suiteBottomInset(),
        LocalMiniPlayerInset provides if (currentMusic != null) 60.dp else 0.dp,
        LocalTabReselect provides tabReselect,
        LocalOpenDownloads provides
            {
                tabReselect.requestDownloads()
                navigator.navigate(HomeRoute)
            },
        LocalPlaybackLauncher provides
            PlaybackLauncher { itemId, itemKind, startFromBeginning ->
                musicPlayerViewModel.pause()
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra("itemId", itemId.toString())
                intent.putExtra("itemKind", itemKind.serialName)
                intent.putExtra("startFromBeginning", startFromBeginning)
                context.startActivity(intent)
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                entries = navigationState.toDecoratedEntries(entryDecorators, entryProvider),
                onBack = { navigator.goBack() },
                transitionSpec = {
                    if (isTabSwitch(tabRoutes)) TAB_TRANSITION_SPEC else PAGE_TRANSITION_SPEC
                },
                popTransitionSpec = {
                    if (isTabSwitch(tabRoutes)) TAB_TRANSITION_SPEC else PAGE_TRANSITION_SPEC
                },
                predictivePopTransitionSpec = { _ ->
                    if (predictiveBack) {
                        PAGE_TRANSITION_SPEC
                    } else {
                        NO_PREVIEW_TRANSITION_SPEC
                    }
                },
            )
            // Predictive back drives NavDisplay's own animation session, whose commit is the
            // predictive transform (a no-op when the preview is off). Intercept the gesture with a
            // plain callback instead, so it commits straight into the normal pop transition -
            // exactly like the in-app back button, and with no preview while dragging.
            if (!predictiveBack) {
                BackHandler(enabled = navigationState.currentBackStack().size > 1) {
                    navigator.goBack()
                }
            }
            val suiteVisible = currentRoute.drawsNavigationSuite(tabRoutes)
            val showMiniPlayer = currentMusic != null
            AnimatedVisibility(
                visible = showMiniPlayer,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Box(
                    modifier =
                        Modifier.padding(
                            start = if (suiteVisible) LocalSuiteStartInset.current else 0.dp,
                            bottom = if (suiteVisible) LocalSuiteBottomInset.current else 0.dp,
                        )
                ) {
                    MiniMusicPlayerHost(viewModel = musicPlayerViewModel)
                }
            }

            // The suite is drawn over the content, outside the NavDisplay, so a page transition
            // never moves it: switching tabs animates only the content behind it. Its presence is
            // animated, so hiding it on immersive pages is a quick fade instead of a pop.
            AnimatedVisibility(
                visible = suiteVisible,
                enter = SUITE_ENTER_TRANSITION,
                exit = SUITE_EXIT_TRANSITION,
                modifier =
                    Modifier.align(
                        if (suiteType == NavigationSuiteType.NavigationRail) {
                            Alignment.CenterStart
                        } else {
                            Alignment.BottomCenter
                        }
                    ),
            ) {
                RootNavigationSuite(
                    navigator = navigator,
                    selectedRoute = topLevelRoute,
                    suiteType = suiteType,
                )
            }
            AppSnackbarHost(
                hostState = snackbarHostState,
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(
                            start = MaterialTheme.spacings.gutter,
                            end = MaterialTheme.spacings.gutter,
                            bottom =
                                MaterialTheme.spacings.medium +
                                    LocalSuiteBottomInset.current +
                                    if (showMiniPlayer) 60.dp else 0.dp,
                        ),
            )
        }
    }
}

@Composable
private fun SetupNavigation(
    backStack: NavBackStack<NavKey>,
    entryDecorators: List<NavEntryDecorator<NavKey>>,
) {
    val goBack: () -> Unit = { backStack.removeLastOrNull() }
    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<OnboardingRoute> { OnboardingScreen(onComplete = {}) }
    }

    NavDisplay(
        backStack = backStack,
        onBack = goBack,
        entryDecorators = entryDecorators,
        entryProvider = entryProvider,
    )
}

private fun EntryProviderScope<NavKey>.mainEntries(
    navigator: Navigator,
    tabReselect: TabReselect,
    onSearchClick: () -> Unit,
    searchExpanded: MutableState<Boolean>,
) {
    entry<HomeRoute> {
        SuiteInsetBox {
            HomeScreen(
                onLibraryClick =
                    resumedAction { library ->
                        when (library.type) {
                            CollectionType.Movies -> navigator.navigate(MoviesRoute)
                            CollectionType.TvShows -> navigator.navigate(ShowsRoute)
                            CollectionType.Books -> {
                                tabReselect.requestBooksTab(0)
                                navigator.navigate(BooksRoute)
                            }
                            else ->
                                navigator.navigate(
                                    LibraryRoute(
                                        libraryId = library.id.toString(),
                                        libraryName = library.name,
                                        libraryType = library.type,
                                    )
                                )
                        }
                    },
                onBooksClick =
                    resumedAction { audiobooks ->
                        tabReselect.requestBooksTab(if (audiobooks) 1 else 0)
                        navigator.navigate(BooksRoute)
                    },
                onSearchClick = dropUnlessResumed { onSearchClick() },
                onSettingsClick =
                    dropUnlessResumed { navigator.navigate(SettingsRoute(indexes = emptyList())) },
                onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
                onSearchResultClick =
                    resumedAction { result -> navigateToSearchResult(navigator, result) },
            )
        }
    }
    entry<MoviesRoute> {
        SuiteInsetBox {
            MoviesScreen(
                onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
                onGenreClick = { genre ->
                    navigator.navigate(
                        GenreListRoute(
                            genreId = genre.id.toString(),
                            genreName = genre.name,
                            movies = true,
                        )
                    )
                },
            )
        }
    }
    entry<GenreListRoute> { key ->
        GenreScreen(
            genreId = UUID.fromString(key.genreId),
            genreName = key.genreName,
            libraryType = if (key.movies) CollectionType.Movies else CollectionType.TvShows,
            onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
            navigateBack = dropUnlessResumed { navigator.goBack() },
        )
    }
    entry<ShowsRoute> {
        SuiteInsetBox {
            ShowsScreen(
                onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
                onGenreClick = { genre ->
                    navigator.navigate(
                        GenreListRoute(
                            genreId = genre.id.toString(),
                            genreName = genre.name,
                            movies = false,
                        )
                    )
                },
            )
        }
    }
    entry<BooksRoute> {
        SuiteInsetBox {
            BooksScreen(onItemClick = resumedAction { item -> navigateToItem(navigator, item) })
        }
    }
    entry<MusicRoute> {
        SuiteInsetBox {
            MusicScreen(
                onArtistClick =
                    resumedAction { artistId -> navigator.navigate(ArtistDetailRoute(artistId)) },
                onAlbumClick =
                    resumedAction { albumId -> navigator.navigate(AlbumDetailRoute(albumId)) },
            )
        }
    }
    entry<ArtistDetailRoute> { key ->
        ArtistDetailScreen(
            artistId = key.artistId,
            navigateBack = dropUnlessResumed { navigator.goBack() },
            onAlbumClick =
                resumedAction { albumId -> navigator.navigate(AlbumDetailRoute(albumId)) },
            navigateToEditImages =
                resumedAction { itemId ->
                    navigator.navigate(EditImagesRoute(itemId = itemId, music = true))
                },
            navigateToIdentify =
                resumedAction { itemId ->
                    navigator.navigate(IdentifyRoute(itemId = itemId, musicKind = "artist"))
                },
        )
    }
    entry<AlbumDetailRoute> { key ->
        AlbumDetailScreen(
            albumId = key.albumId,
            navigateBack = dropUnlessResumed { navigator.goBack() },
            onArtistClick =
                resumedAction { artistId -> navigator.navigate(ArtistDetailRoute(artistId)) },
            navigateToEditImages =
                resumedAction { itemId ->
                    navigator.navigate(EditImagesRoute(itemId = itemId, music = true))
                },
            navigateToIdentify =
                resumedAction { itemId ->
                    navigator.navigate(IdentifyRoute(itemId = itemId, musicKind = "album"))
                },
        )
    }
    entry<MediaRoute> {
        MediaScreen(
            onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
            onSettingsClick = dropUnlessResumed { navigator.navigate(SETTINGS_ROOT_ROUTE) },
            searchExpanded = searchExpanded.value,
            onSearchExpand = { searchExpanded.value = it },
        )
    }
    entry<DownloadsRoute> {
        DownloadsScreen(
            onItemClick =
                resumedAction { item ->
                    navigateToItem(
                        navigator,
                        item,
                    )
                }
        )
    }
    entry<LibraryRoute> { key ->
        LibraryScreen(
            libraryId = UUID.fromString(key.libraryId),
            libraryName = key.libraryName,
            libraryType = key.libraryType,
            onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
            navigateBack = dropUnlessResumed { navigator.goBack() },
        )
    }
    entry<BookDetailRoute> { key ->
        val context = LocalContext.current
        BookDetailScreen(
            bookId = UUID.fromString(key.bookId),
            navigateBack = dropUnlessResumed { navigator.goBack() },
            onRead = { item, startFromBeginning ->
                context.startActivity(
                    ReaderActivity.intent(
                        context = context,
                        bookId = item.id,
                        title = item.name,
                        overview = item.overview,
                        audiobook = item.isAudiobook,
                        startFromBeginning = startFromBeginning,
                    )
                )
            },
            navigateToDownloads = dropUnlessResumed { navigator.navigate(DownloadsRoute) },
            onAuthorClick = { personId ->
                navigator.navigate(PersonRoute(personId.toString(), isAuthor = true))
            },
        )
    }
    entry<CollectionRoute> { key ->
        CollectionScreen(
            collectionId = UUID.fromString(key.collectionId),
            collectionName = key.collectionName,
            onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
            navigateBack = dropUnlessResumed { navigator.goBack() },
            navigateToEditImages =
                resumedAction { itemId ->
                    navigator.navigate(EditImagesRoute(itemId = itemId.toString()))
                },
        )
    }
    entry<MovieRoute> { key ->
        MovieScreen(
            movieId = UUID.fromString(key.movieId),
            navigateBack = dropUnlessResumed { navigator.goBack() },
            navigateHome = dropUnlessResumed { navigator.navigateHome() },
            navigateToPerson =
                resumedAction { personId ->
                    navigator.navigate(PersonRoute(personId = personId.toString()))
                },
            navigateToSubtitleSearch =
                resumedAction { itemId ->
                    navigator.navigate(SubtitleSearchRoute(itemId = itemId.toString()))
                },
            navigateToIdentify =
                resumedAction { itemId ->
                    navigator.navigate(IdentifyRoute(itemId = itemId.toString()))
                },
            navigateToEditImages =
                resumedAction { itemId ->
                    navigator.navigate(EditImagesRoute(itemId = itemId.toString()))
                },
            navigateToDownloads = dropUnlessResumed { navigator.navigate(DownloadsRoute) },
        )
    }
    entry<ShowRoute> { key ->
        ShowScreen(
            showId = UUID.fromString(key.showId),
            navigateBack = dropUnlessResumed { navigator.goBack() },
            navigateHome = dropUnlessResumed { navigator.navigateHome() },
            navigateToItem =
                resumedAction { item ->
                    navigateToItem(
                        navigator,
                        item,
                    )
                },
            navigateToPerson =
                resumedAction { personId ->
                    navigator.navigate(PersonRoute(personId = personId.toString()))
                },
            navigateToIdentify =
                resumedAction { itemId ->
                    navigator.navigate(IdentifyRoute(itemId = itemId.toString(), series = true))
                },
            navigateToEditImages =
                resumedAction { itemId ->
                    navigator.navigate(EditImagesRoute(itemId = itemId.toString()))
                },
        )
    }
    entry<SeasonRoute> { key ->
        SeasonScreen(
            seasonId = UUID.fromString(key.seasonId),
            navigateBack = dropUnlessResumed { navigator.goBack() },
            navigateHome = dropUnlessResumed { navigator.navigateHome() },
            navigateToItem =
                resumedAction { item ->
                    navigateToItem(
                        navigator,
                        item,
                    )
                },
            navigateToSeries =
                resumedAction { seriesId ->
                    navigator.navigateOrReuse(ShowRoute(showId = seriesId.toString()))
                },
            navigateToEditImages =
                resumedAction { itemId ->
                    navigator.navigate(EditImagesRoute(itemId = itemId.toString()))
                },
        )
    }
    entry<EpisodeRoute> { key ->
        EpisodeScreen(
            episodeId = UUID.fromString(key.episodeId),
            navigateBack = dropUnlessResumed { navigator.goBack() },
            navigateHome = dropUnlessResumed { navigator.navigateHome() },
            navigateToPerson =
                resumedAction { personId ->
                    navigator.navigate(PersonRoute(personId = personId.toString()))
                },
            navigateToSeason =
                resumedAction { seasonId ->
                    navigator.navigateOrReuse(SeasonRoute(seasonId = seasonId.toString()))
                },
            navigateToShow =
                resumedAction { seriesId ->
                    navigator.navigateOrReuse(ShowRoute(showId = seriesId.toString()))
                },
            navigateToSubtitleSearch =
                resumedAction { itemId ->
                    navigator.navigate(SubtitleSearchRoute(itemId = itemId.toString()))
                },
            navigateToEditImages =
                resumedAction { itemId ->
                    navigator.navigate(EditImagesRoute(itemId = itemId.toString()))
                },
            navigateToDownloads = dropUnlessResumed { navigator.navigate(DownloadsRoute) },
        )
    }
    entry<SubtitleSearchRoute> { key ->
        SubtitleSearchScreen(
            itemId = UUID.fromString(key.itemId),
            navigateBack = dropUnlessResumed { navigator.goBack() },
        )
    }
    entry<IdentifyRoute> { key ->
        IdentifyScreen(
            itemId = UUID.fromString(key.itemId),
            navigateBack = dropUnlessResumed { navigator.goBack() },
            musicKind = key.musicKind,
            onSearch = { query, year, imdbId, tmdbId, tvdbId, musicBrainzId, discogsId ->
                navigator.navigate(
                    IdentifyResultsRoute(
                        itemId = key.itemId,
                        query = query,
                        year = year,
                        imdbId = imdbId,
                        tmdbId = tmdbId,
                        tvdbId = tvdbId,
                        musicBrainzId = musicBrainzId,
                        discogsId = discogsId,
                        series = key.series,
                        musicKind = key.musicKind,
                    )
                )
            },
        )
    }
    entry<IdentifyResultsRoute> { key ->
        IdentifyResultsScreen(
            itemId = UUID.fromString(key.itemId),
            query = key.query,
            year = key.year,
            imdbId = key.imdbId,
            tmdbId = key.tmdbId,
            tvdbId = key.tvdbId,
            musicBrainzId = key.musicBrainzId,
            discogsId = key.discogsId,
            series = key.series,
            musicKind = key.musicKind,
            navigateBack = dropUnlessResumed { navigator.goBack() },
            onApplied = {
                // Return to the media detail page (form + results are on the stack).
                navigator.goBack()
                navigator.goBack()
            },
        )
    }
    entry<EditImagesRoute> { key ->
        EditImagesScreen(
            itemId = UUID.fromString(key.itemId),
            music = key.music,
            navigateBack = dropUnlessResumed { navigator.goBack() },
            navigateToImageSearch =
                resumedAction { type, isEpisode ->
                    navigator.navigate(
                        EditImageSearchRoute(
                            itemId = key.itemId,
                            type = type.name,
                            isEpisode = isEpisode,
                            music = key.music,
                        )
                    )
                },
        )
    }
    entry<EditImageSearchRoute> { key ->
        EditImageSearchScreen(
            itemId = UUID.fromString(key.itemId),
            type = ImageType.valueOf(key.type),
            isEpisode = key.isEpisode,
            music = key.music,
            navigateBack = dropUnlessResumed { navigator.goBack() },
        )
    }
    entry<PersonRoute> { key ->
        PersonScreen(
            personId = UUID.fromString(key.personId),
            isAuthor = key.isAuthor,
            navigateBack = dropUnlessResumed { navigator.goBack() },
            navigateHome = dropUnlessResumed { navigator.navigateHome() },
            navigateToItem =
                resumedAction { item ->
                    navigateToItem(
                        navigator,
                        item,
                    )
                },
        )
    }
    // Every settings page keeps the navigation suite, so every one of them reserves its space:
    // a constant inset keeps the bar in place instead of laying one out per page.
    entry<SettingsRoute> { key ->
        SuiteInsetBox {
            SettingsScreen(
                indexes = key.indexes,
                navigateToSettings =
                    resumedAction { indexes ->
                        navigator.navigate(SettingsRoute(indexes = indexes))
                    },
                navigateToSettingsFileEdit =
                    resumedAction { filePath ->
                        navigator.navigate(SettingsFileEditRoute(filePath = filePath))
                    },
                navigateToAbout = dropUnlessResumed { navigator.navigate(AboutRoute) },
                navigateToSubtitleFont =
                    dropUnlessResumed { navigator.navigate(SubtitleFontRoute) },
                navigateToReaderFont = dropUnlessResumed { navigator.navigate(ReaderFontRoute) },
                navigateBack = dropUnlessResumed { navigator.goBack() },
                profileHeader = {
                    SettingsProfileHeader(
                        onChangeProfile =
                            dropUnlessResumed { navigator.navigate(ProfilePickerRoute()) }
                    )
                },
            )
        }
    }
    entry<ProfilePickerRoute> { key ->
        ProfilePickerScreen(
            onBack = if (key.canGoBack) dropUnlessResumed { navigator.goBack() } else null,
            onAddProfile = dropUnlessResumed { navigator.navigate(AddProfileRoute) },
            canGoBack = key.canGoBack,
        )
    }
    entry<AddProfileRoute> { AddProfileScreen(onBack = dropUnlessResumed { navigator.goBack() }) }
    entry<SettingsFileEditRoute> { key ->
        SuiteInsetBox {
            SettingsFileEditScreen(
                filePath = key.filePath,
                navigateBack = dropUnlessResumed { navigator.goBack() },
            )
        }
    }
    entry<AboutRoute> {
        SuiteInsetBox { AboutScreen(navigateBack = dropUnlessResumed { navigator.goBack() }) }
    }
    entry<SubtitleFontRoute> {
        SuiteInsetBox {
            SubtitleFontScreen(navigateBack = dropUnlessResumed { navigator.goBack() })
        }
    }
    entry<ReaderFontRoute> {
        SuiteInsetBox { ReaderFontScreen(navigateBack = dropUnlessResumed { navigator.goBack() }) }
    }
}

private fun navigateToSearchResult(navigator: Navigator, result: HomeSearchResult) {
    when (result.kind) {
        BaseItemKind.MOVIE -> navigator.navigate(MovieRoute(result.id.toString()))
        BaseItemKind.SERIES -> navigator.navigate(ShowRoute(result.id.toString()))
        BaseItemKind.EPISODE -> navigator.navigate(EpisodeRoute(result.id.toString()))
        BaseItemKind.SEASON -> navigator.navigate(SeasonRoute(result.id.toString()))
        BaseItemKind.PERSON -> navigator.navigate(PersonRoute(result.id.toString()))
        BaseItemKind.BOOK ->
            navigator.navigate(BookDetailRoute(bookId = result.id.toString(), audiobook = false))
        BaseItemKind.AUDIO_BOOK ->
            navigator.navigate(BookDetailRoute(bookId = result.id.toString(), audiobook = true))
        BaseItemKind.BOX_SET ->
            navigator.navigate(
                CollectionRoute(
                    collectionId = result.id.toString(),
                    collectionName = result.name,
                )
            )
        BaseItemKind.MUSIC_ARTIST ->
            navigator.navigate(ArtistDetailRoute(artistId = result.id.toString()))
        BaseItemKind.MUSIC_ALBUM ->
            navigator.navigate(AlbumDetailRoute(albumId = result.id.toString()))
        else -> Unit
    }
}

private fun navigateToItem(navigator: Navigator, item: FindroidItem) {
    when (item) {
        is FindroidBook ->
            navigator.navigate(
                BookDetailRoute(bookId = item.id.toString(), audiobook = item.isAudiobook)
            )
        is FindroidBoxSet ->
            navigator.navigate(
                CollectionRoute(collectionId = item.id.toString(), collectionName = item.name)
            )
        is FindroidMovie -> navigator.navigate(MovieRoute(movieId = item.id.toString()))
        is FindroidShow -> navigator.navigate(ShowRoute(showId = item.id.toString()))
        is FindroidSeason -> navigator.navigate(SeasonRoute(seasonId = item.id.toString()))
        is FindroidEpisode -> navigator.navigate(EpisodeRoute(episodeId = item.id.toString()))
        is FindroidCollection ->
            navigator.navigate(
                LibraryRoute(
                    libraryId = item.id.toString(),
                    libraryName = item.name,
                    libraryType = item.type,
                )
            )
        is FindroidFolder ->
            navigator.navigate(
                LibraryRoute(
                    libraryId = item.id.toString(),
                    libraryName = item.name,
                    libraryType = CollectionType.Folders,
                )
            )
    }
}
