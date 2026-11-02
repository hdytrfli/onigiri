package dev.hdytrfli.onigiri.feature.film.presentation

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.components.OfflineNotice
import dev.hdytrfli.onigiri.feature.film.presentation.editimages.EditImageSearchEvent
import dev.hdytrfli.onigiri.feature.film.presentation.editimages.EditImageSearchState
import dev.hdytrfli.onigiri.feature.film.presentation.editimages.EditImageSearchViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.editimages.imageTypeAspectRatio
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.PageScaffold
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.utils.ObserveAsEvents
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
    isEpisode: Boolean,
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
        isEpisode = isEpisode,
        state = state,
        onApply = { result -> viewModel.apply(itemId, type, result) },
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditImageSearchLayout(
    type: ImageType,
    isEpisode: Boolean,
    state: EditImageSearchState,
    onApply: (RemoteImageInfo) -> Unit,
    navigateBack: () -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // A poster-shaped result and a wide (16:9) result are different tile shapes, so they use the
    // matching grid size.
    val gridSize =
        if (isPosterShapedSearchResult(type, isEpisode)) GridSize.POSTER else GridSize.THUMBNAIL

    PageScaffold(
        topBar = {
            PageTopBar(
                title = stringResource(CoreR.string.search_images),
                onBack = navigateBack,
            )
        }
    ) { innerPadding ->
        if (LocalOfflineMode.current) {
            OfflineNotice(modifier = Modifier.padding(innerPadding))
            return@PageScaffold
        }
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isSearching ->
                    AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
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
                        columns = GridCells.Fixed(gridColumns(gridSize)),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(all = MaterialTheme.spacings.gutter),
                        horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                        verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                    ) {
                        items(
                            items = state.results,
                            // A banner is wide, so give it two columns in landscape (one in
                            // portrait) rather than changing its aspect ratio.
                            span = { _ ->
                                GridItemSpan(if (type == ImageType.BANNER && isLandscape) 2 else 1)
                            },
                        ) { result ->
                            RemoteImageTile(
                                type = type,
                                isEpisode = isEpisode,
                                result = result,
                                onClick = { onApply(result) },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun RemoteImageTile(
    type: ImageType,
    isEpisode: Boolean,
    result: RemoteImageInfo,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // The image is the card: rounded and tappable on its own. The text sits outside the clip,
        // so the radius cannot crop it.
        AsyncImage(
            model = result.thumbnailUrl ?: result.url,
            contentDescription = null,
            contentScale = searchImageContentScale(type),
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(remoteImageAspectRatio(type, result, isEpisode))
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = result.url != null, onClick = onClick),
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
 * The preview ratio for a result. A banner keeps its own ratio (derived from the result's real
 * dimensions); every other type uses the editor's shape. Box is shown like Primary (a poster),
 * since provider box images are usually posters.
 */
private fun remoteImageAspectRatio(
    type: ImageType,
    image: RemoteImageInfo,
    isEpisode: Boolean,
): Float {
    if (type == ImageType.BANNER) {
        val width = image.width
        val height = image.height
        if (width != null && height != null && width > 0 && height > 0) {
            return width.toFloat() / height.toFloat()
        }
    }
    return searchImageAspectRatio(type, isEpisode)
}

private fun searchImageAspectRatio(type: ImageType, isEpisode: Boolean): Float =
    if (type == ImageType.BOX) imageTypeAspectRatio(ImageType.PRIMARY)
    else imageTypeAspectRatio(type, isEpisode)

/** Whether a result of [type] uses the poster shape rather than a wide (16:9) one. */
private fun isPosterShapedSearchResult(type: ImageType, isEpisode: Boolean): Boolean =
    when (type) {
        ImageType.PRIMARY -> !isEpisode
        ImageType.BOX -> true
        else -> false
    }

/** Poster and square previews fill their container; wide previews are shown whole (no cropping). */
private fun searchImageContentScale(type: ImageType): ContentScale =
    when (type) {
        ImageType.PRIMARY,
        ImageType.BOX,
        ImageType.DISC -> ContentScale.Crop
        else -> ContentScale.Fit
    }
