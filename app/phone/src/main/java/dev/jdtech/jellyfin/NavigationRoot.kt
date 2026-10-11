package dev.jdtech.jellyfin

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidBoxSet
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidFolder
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.navigation.navigateOrReuse
import dev.jdtech.jellyfin.navigation.resumedAction
import dev.jdtech.jellyfin.presentation.film.CollectionScreen
import dev.jdtech.jellyfin.presentation.film.DownloadsScreen
import dev.jdtech.jellyfin.presentation.film.EpisodeScreen
import dev.jdtech.jellyfin.presentation.film.FavoritesScreen
import dev.jdtech.jellyfin.presentation.film.HomeScreen
import dev.jdtech.jellyfin.presentation.film.LibraryScreen
import dev.jdtech.jellyfin.presentation.film.MediaScreen
import dev.jdtech.jellyfin.presentation.film.MovieScreen
import dev.jdtech.jellyfin.presentation.film.PersonScreen
import dev.jdtech.jellyfin.presentation.film.SeasonScreen
import dev.jdtech.jellyfin.presentation.film.ShowScreen
import dev.jdtech.jellyfin.presentation.settings.AboutScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsFileEditScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsScreen
import dev.jdtech.jellyfin.presentation.setup.addresses.ServerAddressesScreen
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.servers.ServersScreen
import dev.jdtech.jellyfin.presentation.setup.users.UsersScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.settings.R as SettingsR
import java.util.UUID

private val TOP_LEVEL_ROUTES = setOf(HomeRoute, MediaRoute, FavoritesRoute, DownloadsRoute)
private val GLOBAL_TRANSITION_SPEC = fadeIn(tween(300)) togetherWith fadeOut(tween(300))

data class TabBarItem(
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val route: NavKey,
    val enabled: Boolean = true,
)

val homeTab =
    TabBarItem(title = CoreR.string.title_home, icon = CoreR.drawable.ic_home, route = HomeRoute)

val mediaTab =
    TabBarItem(
        title = CoreR.string.title_media,
        icon = CoreR.drawable.ic_library,
        route = MediaRoute,
    )

val downloadsTab =
    TabBarItem(
        title = CoreR.string.title_download,
        icon = CoreR.drawable.ic_download,
        route = DownloadsRoute,
    )

val favoritesTab =
    TabBarItem(
        title = CoreR.string.title_favorite,
        icon = CoreR.drawable.ic_heart,
        route = FavoritesRoute,
    )

@Composable
fun NavigationRoot(
    hasServers: Boolean,
    hasCurrentServer: Boolean,
    hasCurrentUser: Boolean,
    confirmExit: Boolean = false,
    navigationAnimation: Boolean = true,
) {
    val setupComplete = hasServers && hasCurrentServer && hasCurrentUser

    val setupStartRoute: NavKey =
        when {
            hasServers && hasCurrentServer -> UsersRoute
            hasServers -> ServersRoute
            else -> WelcomeRoute
        }

    // Navigation state for both modes is hoisted so that it survives switching between them.
    val setupBackStack = rememberNavBackStack(setupStartRoute)
    val navigationState =
        rememberNavigationState(
            startRoute = HomeRoute,
            topLevelRoutes = TOP_LEVEL_ROUTES,
        )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    // The entry decorators hold the per-entry SavedState and ViewModel stores. They must be
    // hoisted as well, otherwise the retained state would be discarded when switching modes.
    val saveableStateHolderDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
    val entryDecorators =
        remember(saveableStateHolderDecorator, viewModelStoreDecorator) {
            listOf(saveableStateHolderDecorator, viewModelStoreDecorator)
        }

    // Exposed as a state object (rather than a plain value) so the entry content closures, which
    // nav3 caches per back stack snapshot, always read the current value.
    val searchExpanded = remember { mutableStateOf(false) }

    // Live setup condition: keep the back stacks in a consistent state across mode switches. The
    // stacks are reconciled during composition (rather than in a LaunchedEffect, which only runs
    // after the first frame has been composed) so the first frame of the new mode already shows
    // the right screen.
    val appliedSetupMode = remember { AppliedSetupMode(setupComplete) }
    if (appliedSetupMode.setupComplete != setupComplete) {
        if (setupComplete) {
            // Setup completed: discard the setup flow and land on Home.
            navigator.navigateHome()
        } else {
            // Setup became incomplete: restart the setup flow at the appropriate screen.
            setupBackStack.clear()
            setupBackStack.add(setupStartRoute)
        }
        appliedSetupMode.setupComplete = setupComplete
    }

    if (setupComplete) {
        MainNavigation(
            navigationState = navigationState,
            navigator = navigator,
            entryDecorators = entryDecorators,
            isOfflineMode = LocalOfflineMode.current,
            searchExpanded = searchExpanded,
            confirmExit = confirmExit,
            navigationAnimation = navigationAnimation,
        )
    } else {
        SetupNavigation(
            backStack = setupBackStack,
            entryDecorators = entryDecorators,
        )
    }
}

/**
 * Remembers the setup mode that the back stacks were last reconciled for. Deliberately not
 * observable state: it is written during composition, which must not schedule recompositions.
 */
private class AppliedSetupMode(var setupComplete: Boolean)

@Composable
private fun MainNavigation(
    navigationState: NavigationState,
    navigator: Navigator,
    entryDecorators: List<NavEntryDecorator<NavKey>>,
    isOfflineMode: Boolean,
    searchExpanded: MutableState<Boolean>,
    confirmExit: Boolean,
    navigationAnimation: Boolean,
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    // Confirm before exiting from the start route's root.
    val atExitRoot =
        navigationState.currentBackStack().size <= 1 &&
            navigationState.topLevelRoute == navigationState.startRoute
    BackHandler(enabled = confirmExit && atExitRoot) { showExitDialog = true }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(SettingsR.string.confirm_exit_dialog_title)) },
            text = { Text(stringResource(SettingsR.string.confirm_exit_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text(stringResource(SettingsR.string.exit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    val navigationItems =
        when (isOfflineMode) {
            false -> listOf(homeTab, mediaTab, favoritesTab, downloadsTab)
            true -> listOf(homeTab, favoritesTab, downloadsTab)
        }
    val tabRoutes = navigationItems.map { it.route }

    val topLevelRoute = navigationState.topLevelRoute
    val atTabRoot = navigationState.currentBackStack().last() == topLevelRoute
    val showBottomBar = atTabRoot && topLevelRoute in tabRoutes && !searchExpanded.value

    // The search bar belongs to the Media tab: collapse it when the active top level route
    // changes away from Media (e.g. via the home button on a detail screen) so that the bottom
    // bar is shown again.
    LaunchedEffect(topLevelRoute) {
        if (topLevelRoute != MediaRoute) {
            searchExpanded.value = false
        }
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val useRail =
        windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
        )

    val entryProvider = entryProvider {
        mainEntries(
            navigator = navigator,
            onSearchClick = {
                searchExpanded.value = true
                navigator.navigate(MediaRoute)
            },
            searchExpanded = searchExpanded,
        )
    }

    val contentTransform =
        if (navigationAnimation) {
            GLOBAL_TRANSITION_SPEC
        } else {
            EnterTransition.None togetherWith ExitTransition.None
        }

    val navDisplay: @Composable (Modifier) -> Unit = { modifier ->
        NavDisplay(
            entries = navigationState.toDecoratedEntries(entryDecorators, entryProvider),
            onBack = { navigator.goBack() },
            modifier = modifier,
            transitionSpec = { contentTransform },
            popTransitionSpec = { contentTransform },
            predictivePopTransitionSpec = { _ -> contentTransform },
        )
    }

    val onTabClick: (NavKey) -> Unit = { route ->
        if (route == MediaRoute && topLevelRoute == MediaRoute) {
            searchExpanded.value = true
        }
        navigator.navigate(route)
    }

    if (useRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showBottomBar,
                enter =
                    if (navigationAnimation) slideInHorizontally(initialOffsetX = { -it })
                    else EnterTransition.None,
                exit =
                    if (navigationAnimation) slideOutHorizontally(targetOffsetX = { -it })
                    else ExitTransition.None,
            ) {
                NavigationRail {
                    navigationItems.forEach { item ->
                        NavigationRailItem(
                            selected = topLevelRoute == item.route,
                            onClick = { onTabClick(item.route) },
                            icon = {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = stringResource(item.title),
                                )
                            },
                            label = { Text(text = stringResource(item.title)) },
                            enabled = item.enabled,
                        )
                    }
                }
            }
            navDisplay(Modifier.weight(1f))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            navDisplay(Modifier.weight(1f))
            AnimatedVisibility(
                visible = showBottomBar,
                enter =
                    if (navigationAnimation) slideInVertically(initialOffsetY = { it })
                    else EnterTransition.None,
                exit =
                    if (navigationAnimation) slideOutVertically(targetOffsetY = { it })
                    else ExitTransition.None,
            ) {
                NavigationBar {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = topLevelRoute == item.route,
                            onClick = { onTabClick(item.route) },
                            icon = {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = stringResource(item.title),
                                )
                            },
                            label = { Text(text = stringResource(item.title)) },
                            enabled = item.enabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupNavigation(
    backStack: NavBackStack<NavKey>,
    entryDecorators: List<NavEntryDecorator<NavKey>>,
) {
    val navigate: (NavKey) -> Unit = backStack::add
    val goBack: () -> Unit = { backStack.removeLastOrNull() }
    val navigateOrReuse: (NavKey) -> Unit = backStack::navigateOrReuse

    val entryProvider = entryProvider {
        setupEntries(
            navigate = navigate,
            goBack = goBack,
            navigateOrReuse = navigateOrReuse,
            showBack = backStack.size > 1,
            onSetupComplete = {},
        )
    }

    NavDisplay(
        backStack = backStack,
        onBack = goBack,
        entryDecorators = entryDecorators,
        entryProvider = entryProvider,
        transitionSpec = { GLOBAL_TRANSITION_SPEC },
        popTransitionSpec = { GLOBAL_TRANSITION_SPEC },
        predictivePopTransitionSpec = { _ -> GLOBAL_TRANSITION_SPEC },
    )
}

/**
 * Registers the setup flow entries.
 *
 * The same entries serve both the dedicated setup mode and the setup flow reached from the main UI
 * (e.g. via "manage servers" in Settings). [onSetupComplete] captures the difference: in setup mode
 * completing the flow flips the live condition to the main UI (a no-op), while in the main UI it
 * navigates home. It is invoked through the nearest entry's RESUMED guard.
 */
private fun EntryProviderScope<NavKey>.setupEntries(
    navigate: (NavKey) -> Unit,
    goBack: () -> Unit,
    navigateOrReuse: (NavKey) -> Unit,
    showBack: Boolean,
    onSetupComplete: () -> Unit,
) {
    entry<WelcomeRoute> {
        WelcomeScreen(onContinueClick = dropUnlessResumed { navigate(ServersRoute) })
    }
    entry<ServersRoute> {
        ServersScreen(
            navigateToUsers = dropUnlessResumed { navigate(UsersRoute) },
            navigateToAddresses =
                resumedAction { serverId -> navigate(ServerAddressesRoute(serverId)) },
            onAddClick = dropUnlessResumed { navigate(AddServerRoute) },
            onBackClick = dropUnlessResumed { goBack() },
            showBack = showBack,
        )
    }
    entry<AddServerRoute> {
        AddServerScreen(
            onSuccess = dropUnlessResumed { navigate(UsersRoute) },
            onBackClick = dropUnlessResumed { goBack() },
        )
    }
    entry<ServerAddressesRoute> { key ->
        ServerAddressesScreen(
            serverId = key.serverId,
            navigateBack = dropUnlessResumed { goBack() },
        )
    }
    entry<UsersRoute> {
        UsersScreen(
            navigateToHome = dropUnlessResumed { onSetupComplete() },
            onChangeServerClick = dropUnlessResumed { navigateOrReuse(ServersRoute) },
            onAddClick = dropUnlessResumed { navigate(LoginRoute()) },
            onBackClick = dropUnlessResumed { goBack() },
            onPublicUserClick =
                resumedAction { username -> navigate(LoginRoute(username = username)) },
            showBack = showBack,
        )
    }
    entry<LoginRoute> { key ->
        LoginScreen(
            onSuccess = dropUnlessResumed { onSetupComplete() },
            onChangeServerClick = dropUnlessResumed { navigateOrReuse(ServersRoute) },
            onBackClick = dropUnlessResumed { goBack() },
            prefilledUsername = key.username,
        )
    }
}

private fun EntryProviderScope<NavKey>.mainEntries(
    navigator: Navigator,
    onSearchClick: () -> Unit,
    searchExpanded: MutableState<Boolean>,
) {
    setupEntries(
        navigate = navigator::navigate,
        goBack = navigator::goBack,
        navigateOrReuse = navigator::navigateOrReuse,
        showBack = true,
        onSetupComplete = { navigator.navigateHome() },
    )
    entry<HomeRoute> {
        HomeScreen(
            onLibraryClick =
                resumedAction { library ->
                    navigator.navigate(
                        LibraryRoute(
                            libraryId = library.id.toString(),
                            libraryName = library.name,
                            libraryType = library.type,
                        )
                    )
                },
            onSearchClick = dropUnlessResumed { onSearchClick() },
            onSettingsClick =
                dropUnlessResumed {
                    navigator.navigate(SettingsRoute(indexes = listOf(CoreR.string.title_settings)))
                },
            onManageServers = dropUnlessResumed { navigator.navigate(ServersRoute) },
            onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
        )
    }
    entry<MediaRoute> {
        MediaScreen(
            onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
            onSettingsClick =
                dropUnlessResumed {
                    navigator.navigate(SettingsRoute(indexes = listOf(CoreR.string.title_settings)))
                },
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
    entry<CollectionRoute> { key ->
        CollectionScreen(
            collectionId = UUID.fromString(key.collectionId),
            collectionName = key.collectionName,
            onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
            navigateBack = dropUnlessResumed { navigator.goBack() },
        )
    }
    entry<FavoritesRoute> {
        FavoritesScreen(
            onItemClick = resumedAction { item -> navigateToItem(navigator, item) },
            onSearchClick =
                dropUnlessResumed {
                    searchExpanded.value = true
                    navigator.navigate(MediaRoute)
                },
            onSettingsClick =
                dropUnlessResumed {
                    navigator.navigate(SettingsRoute(indexes = listOf(CoreR.string.title_settings)))
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
        )
    }
    entry<PersonRoute> { key ->
        PersonScreen(
            personId = UUID.fromString(key.personId),
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
    entry<SettingsRoute> { key ->
        SettingsScreen(
            indexes = key.indexes,
            navigateToSettings =
                resumedAction { indexes -> navigator.navigate(SettingsRoute(indexes = indexes)) },
            navigateToSettingsFileEdit =
                resumedAction { filePath ->
                    navigator.navigate(SettingsFileEditRoute(filePath = filePath))
                },
            navigateToServers = dropUnlessResumed { navigator.navigate(ServersRoute) },
            navigateToUsers = dropUnlessResumed { navigator.navigate(UsersRoute) },
            navigateToAbout = dropUnlessResumed { navigator.navigate(AboutRoute) },
            navigateBack = dropUnlessResumed { navigator.goBack() },
        )
    }
    entry<SettingsFileEditRoute> { key ->
        SettingsFileEditScreen(
            filePath = key.filePath,
            navigateBack = dropUnlessResumed { navigator.goBack() },
        )
    }
    entry<AboutRoute> { AboutScreen(navigateBack = dropUnlessResumed { navigator.goBack() }) }
}

private fun navigateToItem(navigator: Navigator, item: FindroidItem) {
    when (item) {
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
        else -> Unit
    }
}
