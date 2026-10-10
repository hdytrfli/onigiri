package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun EpisodeGridCard(episode: FindroidEpisode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)) {
        Box {
            ItemPoster(
                item = episode,
                direction = Direction.HORIZONTAL,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small),
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(MaterialTheme.spacings.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                if (episode.isDownloaded()) DownloadedBadge()
                if (episode.played) PlayedBadge()
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
        Text(
            text = episode.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EpisodeGridCardPreview() {
    FindroidTheme { EpisodeGridCard(episode = dummyEpisode, onClick = {}) }
}
