package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.film.presentation.media.MediaKind
import dev.jdtech.jellyfin.film.presentation.media.MediaSuggestionsState
import dev.jdtech.jellyfin.film.presentation.media.MediaSuggestionsViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.film.components.ErrorCard
import dev.jdtech.jellyfin.presentation.film.components.MediaSectionRow
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

/** The Movies root page: continue watching, newly added, then Jellyfin's recommendations. */
@Composable
fun MoviesScreen(
    onItemClick: (FindroidItem) -> Unit,
    onListClick: () -> Unit,
    viewModel: MediaSuggestionsViewModel = hiltViewModel(),
) {
    MediaSuggestionsScreen(MediaKind.Movies, onItemClick, onListClick, viewModel)
}

/** The Shows root page: continue watching, latest episodes and next up. */
@Composable
fun ShowsScreen(
    onItemClick: (FindroidItem) -> Unit,
    onListClick: () -> Unit,
    viewModel: MediaSuggestionsViewModel = hiltViewModel(),
) {
    MediaSuggestionsScreen(MediaKind.Shows, onItemClick, onListClick, viewModel)
}

@Composable
private fun MediaSuggestionsScreen(
    kind: MediaKind,
    onItemClick: (FindroidItem) -> Unit,
    onListClick: () -> Unit,
    viewModel: MediaSuggestionsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(kind) { viewModel.load(kind) }

    MediaSuggestionsLayout(
        state = state,
        onItemClick = onItemClick,
        onListClick = onListClick,
        onRetry = { viewModel.retry(kind) },
    )
}

@Composable
private fun MediaSuggestionsLayout(
    state: MediaSuggestionsState,
    onItemClick: (FindroidItem) -> Unit,
    onListClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)
    val contentPadding =
        PaddingValues(
            top = safePadding.top + MaterialTheme.spacings.small,
            bottom = safePadding.bottom + MaterialTheme.spacings.default,
        )

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
            ErrorCard(
                onShowStacktrace = {},
                onRetryClick = onRetry,
                modifier =
                    Modifier.align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacings.default),
            )
        } else if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
