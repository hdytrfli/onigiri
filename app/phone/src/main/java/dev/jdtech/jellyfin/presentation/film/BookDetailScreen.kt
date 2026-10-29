package dev.jdtech.jellyfin.presentation.film

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.film.presentation.book.BookDetailViewModel
import dev.jdtech.jellyfin.models.FindroidBook
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.PageContent
import dev.jdtech.jellyfin.presentation.components.PageScaffold
import dev.jdtech.jellyfin.presentation.components.PageTopBar
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.util.UUID

/** A book's detail page: cover, description, and the action that opens the reader. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: UUID,
    navigateBack: () -> Unit,
    onRead: (FindroidBook) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(bookId) { viewModel.load(bookId) }

    PageScaffold(
        topBar = {
            PageTopBar(
                title = state.item?.name.orEmpty(),
                onBack = navigateBack,
            )
        }
    ) { innerPadding ->
        PageContent(modifier = Modifier.padding(innerPadding)) {
            when {
                state.error != null ->
                    FullPageError(
                        onRefresh = { viewModel.load(bookId) },
                        isRefreshing = false,
                        error = state.error,
                        modifier = Modifier.fillMaxSize(),
                    )
                state.isLoading || state.item == null ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                else -> {
                    val item = state.item!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                bottom = MaterialTheme.spacings.gutter
                            ),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                    ) {
                        item { BookBackdrop(item = item) }
                        item {
                            Column(
                                modifier =
                                    Modifier.padding(horizontal = MaterialTheme.spacings.gutter),
                                verticalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.small),
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                if (item.isAudiobook) {
                                    Text(
                                        text = "Audiobook",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (item.overview.isNotBlank()) {
                                    Text(
                                        text = item.overview,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                AppButton(onClick = { onRead(item) }) {
                                    Text(if (item.isAudiobook) "Play" else "Read")
                                }
                                if (item.isAudiobook) {
                                    Text(
                                        text =
                                            "Audiobook playback isn't supported in the reader yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Books have no backdrop image. Portrait uses the cover as a tall header; landscape puts the cover
 * on the right with the left side black, fading into the image where they meet.
 */
@Composable
fun BookBackdrop(item: FindroidBook, modifier: Modifier = Modifier) {
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    if (portrait) {
        AsyncImage(
            model = item.images.primary,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
        )
    } else {
        Box(modifier = modifier.fillMaxWidth().height(340.dp).background(Color.Black)) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight())
                AsyncImage(
                    model = item.images.primary,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                )
            }
            // Blend the black column into the cover instead of a hard seam.
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.5f to Color.Black,
                                0.68f to Color.Transparent,
                            )
                        )
            )
        }
    }
}
