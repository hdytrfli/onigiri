package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.media.GenresViewModel
import dev.jdtech.jellyfin.film.presentation.media.MediaKind
import dev.jdtech.jellyfin.film.presentation.media.MediaSuggestionsState
import dev.jdtech.jellyfin.film.presentation.media.MediaSuggestionsViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidGenre
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
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

    LaunchedEffect(kind) { viewModel.load(kind) }

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
        )
        when (selectedTab) {
            0 ->
                LibraryScreen(
                    libraryId = null,
                    libraryName = null,
                    libraryType =
                        if (kind == MediaKind.Movies) CollectionType.Movies
                        else CollectionType.TvShows,
                    onItemClick = onItemClick,
                    navigateBack = {},
                    showTopBar = false,
                    embedded = true,
                )
            1 ->
                MediaSuggestionsContent(
                    state = state,
                    onItemClick = onItemClick,
                    onListClick = { selectedTab = 0 },
                    onRetry = { viewModel.retry(kind) },
                )
            else -> GenresContent(kind = kind, onGenreClick = onGenreClick)
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
            )
        } else if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun GenresContent(
    kind: MediaKind,
    onGenreClick: (FindroidGenre) -> Unit,
    viewModel: GenresViewModel = hiltViewModel(),
) {
    val genres by viewModel.state.collectAsStateWithLifecycle()
    val safePadding = rememberSafePadding(handleStartInsets = false)

    LaunchedEffect(kind) { viewModel.load(kind) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = safePadding.start + MaterialTheme.spacings.default,
                top = MaterialTheme.spacings.small,
                end = safePadding.end + MaterialTheme.spacings.default,
                bottom = safePadding.bottom + MaterialTheme.spacings.default,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        gridItems(items = genres, key = { it.id }) { genre ->
            AppButton(
                onClick = { onGenreClick(genre) },
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Text(
                        text = genre.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
