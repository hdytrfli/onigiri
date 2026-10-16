package dev.jdtech.jellyfin.presentation.film

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.editimages.EditImageSearchEvent
import dev.jdtech.jellyfin.film.presentation.editimages.EditImageSearchState
import dev.jdtech.jellyfin.film.presentation.editimages.EditImageSearchViewModel
import dev.jdtech.jellyfin.film.presentation.editimages.imageTypeAspectRatio
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import java.util.UUID
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.RemoteImageInfo

/**
 * The remote-image results for one image type: a full-width adaptive grid (movie-list style).
 * Tapping an image applies it (the server downloads it) and returns to the Edit images page.
 */
@Composable
fun EditImageSearchScreen(
    itemId: UUID,
    type: ImageType,
    navigateBack: () -> Unit,
    viewModel: EditImageSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(itemId, type) { viewModel.search(itemId, type) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EditImageSearchEvent.Applied -> navigateBack()
        }
    }

    EditImageSearchLayout(
        type = type,
        state = state,
        onApply = { result -> viewModel.apply(itemId, type, result) },
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditImageSearchLayout(
    type: ImageType,
    state: EditImageSearchState,
    onApply: (RemoteImageInfo) -> Unit,
    navigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Fewer, larger columns than the Identify grid: 3 in portrait, 4 in landscape.
    val columns = if (isLandscape) 4 else 3

    Scaffold(
        modifier =
            Modifier.fillMaxSize()
                .recalculateWindowInsets()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreR.string.search_images)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = topBarWindowInsets(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isSearching ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null ->
                    Text(
                        text = stringResource(CoreR.string.identify_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                state.results.isEmpty() ->
                    Text(
                        text = stringResource(CoreR.string.identify_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                else ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(all = MaterialTheme.spacings.default),
                        horizontalArrangement =
                            Arrangement.spacedBy(MaterialTheme.spacings.default),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    ) {
                        items(
                            items = state.results,
                            key = { it.hashCode() },
                            // A banner is wide, so give it two columns in landscape (one in
                            // portrait) rather than changing its aspect ratio.
                            span = { _ ->
                                GridItemSpan(if (type == ImageType.BANNER && isLandscape) 2 else 1)
                            },
                        ) { result ->
                            RemoteImageTile(
                                result = result,
                                ratio = searchImageAspectRatio(type),
                                onClick = { onApply(result) },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun RemoteImageTile(result: RemoteImageInfo, ratio: Float, onClick: () -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = result.url != null, onClick = onClick)
    ) {
        AsyncImage(
            model = result.thumbnailUrl ?: result.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        Text(
            text = result.providerName.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        remoteImageResolution(result)
            .takeIf { it.isNotEmpty() }
            ?.let { resolution ->
                Text(
                    text = resolution,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

private fun remoteImageResolution(image: RemoteImageInfo): String =
    if (image.width != null && image.height != null) "${image.width}×${image.height}" else ""

/**
 * The preview ratio for [type] in the results grid. Box is shown like Primary (a poster), since
 * provider box images are usually posters; everything else uses the editor's ratio.
 */
private fun searchImageAspectRatio(type: ImageType): Float =
    if (type == ImageType.BOX) imageTypeAspectRatio(ImageType.PRIMARY)
    else imageTypeAspectRatio(type)
