package dev.jdtech.jellyfin.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.ScrimLight
import dev.jdtech.jellyfin.film.presentation.mediadetail.MediaDetailState
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.presentation.film.components.InfoText
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.film.components.VideoMetadataBar
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalBackdropRatios

/**
 * Everything shared by the media-detail, episodes and playback-options sheets: backdrop, title,
 * year/runtime/ratings, video metadata pills, description and the genres/director/writers table.
 */
@Composable
fun MediaDetailHeader(detail: MediaDetailState) {
    if (detail.isLoading) return

    val backdropUri = detail.backdropUri
    Column(modifier = Modifier.fillMaxWidth()) {
        if (backdropUri != null) {
            BackdropHeader(backdropUri = backdropUri, title = detail.title)
        }
        Column(
            modifier =
                Modifier.padding(
                    start = MaterialTheme.spacings.default,
                    top =
                        if (backdropUri == null) MaterialTheme.spacings.default
                        else MaterialTheme.spacings.medium,
                    end = MaterialTheme.spacings.default,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            if (backdropUri == null) {
                detail.title?.let { title ->
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                }
            }
            MetadataRow(detail)
            detail.videoMetadata?.let { videoMetadata -> VideoMetadataBar(videoMetadata) }
            detail.overview
                ?.takeIf { it.isNotBlank() }
                ?.let { overview -> OverviewText(text = overview, maxCollapsedLines = 4) }
            if (
                detail.genres.isNotEmpty() || detail.director != null || detail.writers.isNotEmpty()
            ) {
                InfoText(
                    genres = detail.genres,
                    director = detail.director,
                    writers = detail.writers,
                )
            }
        }
        // Separates the shared header from sheet-specific content.
        Spacer(Modifier.height(MaterialTheme.spacings.default))
    }
}

/** Read-only media details shown in the player, mirroring the movie/episode detail page. */
@Composable
fun MediaDetailContent(detail: MediaDetailState) {
    if (detail.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MediaDetailHeader(detail)

        if (detail.actors.isNotEmpty()) {
            Column(
                modifier =
                    Modifier.padding(
                        start = MaterialTheme.spacings.default,
                        end = MaterialTheme.spacings.default,
                        bottom = MaterialTheme.spacings.default,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                Text(
                    text = stringResource(CoreR.string.cast_amp_crew),
                    style = MaterialTheme.typography.titleMedium,
                )
                detail.actors.forEach { person -> PersonRow(person = person) }
            }
        }
    }
}

@Composable
private fun MetadataRow(detail: MediaDetailState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        detail.year?.let { year ->
            Text(text = year.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        detail.runtimeMinutes?.let { runtime ->
            Text(
                text = stringResource(CoreR.string.runtime_minutes, runtime),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        detail.officialRating?.let { rating ->
            Text(text = rating, style = MaterialTheme.typography.bodyMedium)
        }
        detail.communityRating?.let { rating ->
            Text(text = "%.1f".format(rating), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BackdropHeader(backdropUri: String, title: String?) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val context = LocalContext.current
    val backdrop =
        remember(context, backdropUri) {
            ImageRequest.Builder(context)
                .data(backdropUri)
                // Backdrops are multi-megabyte bitmaps: served from disk, not the heap.
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
        }
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(LocalBackdropRatios.current.playerSheet)
                .clipToBounds()
    ) {
        AsyncImage(
            model = backdrop,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(ScrimLight)
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, backgroundColor),
                        startY = 0f,
                    )
            )
        }
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier.align(Alignment.BottomStart)
                        .padding(
                            start = MaterialTheme.spacings.default,
                            end = MaterialTheme.spacings.default,
                            bottom = MaterialTheme.spacings.medium,
                        ),
            )
        }
    }
}

@Composable
private fun PersonRow(person: FindroidItemPerson) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val context = LocalContext.current
        val avatar =
            remember(context, person.image.uri) {
                ImageRequest.Builder(context)
                    .data(person.image.uri)
                    // Avatars are not worth a slot in the shared bitmap pool: the disk cache
                    // serves them and they decode small.
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            }
        AsyncImage(
            model = avatar,
            contentDescription = null,
            modifier =
                Modifier.size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (person.role.isNotBlank()) {
                Text(
                    text = person.role,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
