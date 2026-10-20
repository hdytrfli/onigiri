package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR
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
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.PageToolbar
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.MediaSectionRow
import dev.jdtech.jellyfin.presentation.theme.spacings
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
    val safePadding = rememberSafePadding(handleStartInsets = false)

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

    val toolbarActions: (@Composable RowScope.() -> Unit)? =
        if (selectedTab == 0) {
            {
                AppIconButton(
                    onClick = { listViewModel.onAction(LibraryAction.ToggleSearch) },
                    variant = AppButtonVariant.Ghost,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_search),
                            contentDescription = stringResource(CoreR.string.search),
                        )
                    },
                )
                AppIconButton(
                    onClick = { showDisplayDialog = true },
                    variant = AppButtonVariant.Ghost,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_layout_list),
                            contentDescription = stringResource(CoreR.string.display_info),
                        )
                    },
                )
                AppIconButton(
                    onClick = { showSortDialog = true },
                    variant = AppButtonVariant.Ghost,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_layout_arrow_down),
                            contentDescription = stringResource(CoreR.string.sort_by),
                        )
                    },
                )
            }
        } else {
            null
        }

    Column(modifier = Modifier.fillMaxSize()) {
        PageToolbar(
            tabs =
                listOf(
                    stringResource(
                        if (kind == MediaKind.Movies) CoreR.string.movies_label
                        else CoreR.string.shows_label
                    ),
                    stringResource(FilmR.string.suggestions),
                    stringResource(FilmR.string.genres),
                ),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier =
                Modifier.padding(
                    top = safePadding.top + MaterialTheme.spacings.small,
                    bottom = MaterialTheme.spacings.small,
                ),
            actions = toolbarActions,
        )
        when (selectedTab) {
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
                    showDisplayDialog = showDisplayDialog,
                    onShowSortDialogChange = { showSortDialog = it },
                    onShowDisplayDialogChange = { showDisplayDialog = it },
                    embedded = true,
                    sectionTitle =
                        stringResource(
                            if (kind == MediaKind.Movies) CoreR.string.all_movies
                            else CoreR.string.all_shows
                        ),
                )
            1 ->
                MediaSuggestionsContent(
                    state = state,
                    onItemClick = onItemClick,
                    onListClick = { selectedTab = 0 },
                    onRetry = { viewModel.retry(kind) },
                )
            else ->
                GenresContent(
                    kind = kind,
                    displayFields = state.displayFields,
                    onItemClick = onItemClick,
                    onGenreClick = onGenreClick,
                )
        }
    }
}

@Composable
private fun MediaSuggestionsContent(
    state: MediaSuggestionsState,
    onItemClick: (FindroidItem) -> Unit,
    onListClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)
    val contentPadding = PaddingValues(bottom = safePadding.bottom + MaterialTheme.spacings.default)

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.sections.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                items(items = state.sections, key = { it.id }) { section ->
                    MediaSectionRow(
                        section = section,
                        displayFields = state.displayFields,
                        onItemClick = onItemClick,
                        onListClick = if (section.showsListLink) onListClick else null,
                    )
                }
            }
        } else if (state.error != null) {
            FullPageError(
                onRefresh = onRetry,
                isRefreshing = state.isLoading,
                error = state.error,
            )
        } else if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
    val safePadding = rememberSafePadding(handleStartInsets = false)

    LaunchedEffect(kind) { viewModel.load(kind) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.error != null && state.sections.isEmpty()) {
            FullPageError(
                onRefresh = { viewModel.retry(kind) },
                isRefreshing = state.isLoading,
                error = state.error,
            )
        } else if (state.isLoading && state.sections.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(bottom = safePadding.bottom + MaterialTheme.spacings.default),
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
