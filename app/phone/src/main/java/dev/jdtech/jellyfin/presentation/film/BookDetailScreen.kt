package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.book.BookDetailViewModel
import dev.jdtech.jellyfin.models.FindroidBook
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.film.components.ActorsRow
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
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
                            ItemTitle(
                                title = item.name,
                                logoUri = item.images.logo,
                                maxLines = 1,
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = paddingStart, end = paddingEnd)) {
                        Spacer(Modifier.height(MaterialTheme.spacings.small))
                        AppButton(onClick = { onRead(item) }) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_book_open),
                                contentDescription = null,
                            )
                            Text(
                                if (item.playbackPositionTicks > 0) {
                                    "Continue reading"
                                } else {
                                    "Read book"
                                }
                            )
                        }
                        Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        OverviewText(text = item.overview)
                        Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        BookInfoRow(
                            label = "Year",
                            value =
                                (item.productionYear ?: item.premiereDate?.year)
                                    ?.toString()
                                    .orEmpty(),
                        )
                        BookInfoRow(label = "Studio", value = item.studios.firstOrNull().orEmpty())
                        if (item.people.isNotEmpty()) {
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        }
                    }
                    if (item.people.isNotEmpty()) {
                        ActorsRow(
                            actors = item.people,
                            onActorClick = {},
                            contentPadding =
                                androidx.compose.foundation.layout.PaddingValues(
                                    start = paddingStart,
                                    end = paddingEnd,
                                ),
                            titleRes = CoreR.string.authors,
                        )
                    }
                }

                ItemTopBar(hasBackButton = true, onBackClick = navigateBack)
            }
        }
    }
}

@Composable
private fun BookInfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
