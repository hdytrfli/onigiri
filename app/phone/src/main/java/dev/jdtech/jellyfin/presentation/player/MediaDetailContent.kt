package dev.jdtech.jellyfin.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.mediadetail.MediaDetailState
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.presentation.film.components.InfoText
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.film.components.VideoMetadataBar
import dev.jdtech.jellyfin.presentation.theme.spacings
import org.jellyfin.sdk.model.api.PersonKind

/** Read-only media details shown in the player, mirroring the movie/episode detail page. */
@Composable
fun MediaDetailContent(detail: MediaDetailState) {
    if (detail.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val actors = remember(detail.people) { detail.people.filter { it.type == PersonKind.ACTOR } }
    val director =
        remember(detail.people) { detail.people.firstOrNull { it.type == PersonKind.DIRECTOR } }
    val writers = remember(detail.people) { detail.people.filter { it.type == PersonKind.WRITER } }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacings.default),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        detail.title?.let { title ->
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        }

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

        detail.videoMetadata?.let { videoMetadata -> VideoMetadataBar(videoMetadata) }

        detail.overview
            ?.takeIf { it.isNotBlank() }
            ?.let { overview -> OverviewText(text = overview, maxCollapsedLines = 4) }

        if (detail.genres.isNotEmpty() || director != null || writers.isNotEmpty()) {
            InfoText(genres = detail.genres, director = director, writers = writers)
        }

        if (actors.isNotEmpty()) {
            Text(
                text = stringResource(CoreR.string.cast_amp_crew),
                style = MaterialTheme.typography.titleMedium,
            )
            actors.forEach { person -> PersonRow(person = person) }
        }

        Spacer(Modifier.size(MaterialTheme.spacings.default))
    }
}

@Composable
private fun PersonRow(person: FindroidItemPerson) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = person.image.uri,
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
