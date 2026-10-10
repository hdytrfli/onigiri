package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun EpisodeGridCard(episode: FindroidEpisode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)) {
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
        Box(
            modifier =
                Modifier.align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(MaterialTheme.spacings.small)
        ) {
            Text(
                text = episode.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EpisodeGridCardPreview() {
    FindroidTheme { EpisodeGridCard(episode = dummyEpisode, onClick = {}) }
}
