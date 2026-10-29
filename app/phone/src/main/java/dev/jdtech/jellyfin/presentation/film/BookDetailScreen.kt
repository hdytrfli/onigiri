package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.film.presentation.book.BookDetailViewModel
import dev.jdtech.jellyfin.models.FindroidBook
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.ItemButtonsBar
import dev.jdtech.jellyfin.presentation.film.components.ItemHeader
import dev.jdtech.jellyfin.presentation.film.components.ItemTitle
import dev.jdtech.jellyfin.presentation.film.components.ItemTopBar
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import java.util.UUID

/** A book's detail page, built from the same components as the movie detail page. */
@Composable
fun BookDetailScreen(
    bookId: UUID,
    navigateBack: () -> Unit,
    onRead: (FindroidBook) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(bookId) { viewModel.load(bookId) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.error != null ->
                FullPageError(
                    onRefresh = { viewModel.load(bookId) },
                    isRefreshing = false,
                    error = state.error,
                    modifier = Modifier.fillMaxSize(),
                )
            state.isLoading || state.item == null ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                val item = state.item!!
                val safePadding = rememberSafePadding()
                val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
                val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
                val scrollState = rememberScrollState()

                Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                    ItemHeader(item = item, scrollState = scrollState) {
                        Column(
                            modifier =
                                Modifier.align(Alignment.BottomStart)
                                    .padding(start = paddingStart, end = paddingEnd)
                        ) {
                            ItemTitle(title = item.name, logoUri = item.images.logo)
                        }
                    }
                    Column(modifier = Modifier.padding(start = paddingStart, end = paddingEnd)) {
                        Spacer(Modifier.height(MaterialTheme.spacings.small))
                        androidx.compose.material3.Text(
                            text = if (item.isAudiobook) "Audiobook" else "Book",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.small))
                        ItemButtonsBar(
                            item = item,
                            onPlayClick = { onRead(item) },
                            onMarkAsPlayedClick = {},
                            onMarkAsFavoriteClick = {},
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        OverviewText(text = item.overview)
                    }
                }

                ItemTopBar(hasBackButton = true, onBackClick = navigateBack)
            }
        }
    }
}
