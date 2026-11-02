package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.models.AudioCodec
import dev.hdytrfli.onigiri.models.DisplayProfile
import dev.hdytrfli.onigiri.models.VideoMetadata
import dev.hdytrfli.onigiri.ui.preview.dummyVideoMetadata
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings

@Composable
fun VideoMetadataBar(videoMetadata: VideoMetadata) {
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
        videoMetadata.resolution.firstOrNull()?.apply { VideoMetadataBarItem(text = this.raw) }
        videoMetadata.videoCodecs.firstOrNull()?.apply { VideoMetadataBarItem(text = this.raw) }
        videoMetadata.displayProfiles.firstOrNull()?.apply {
            val icon =
                when (this) {
                    DisplayProfile.DOLBY_VISION -> CoreR.drawable.ic_dolby
                    else -> null
                }
            VideoMetadataBarItem(text = this.raw, icon = icon)
        }
        videoMetadata.audioCodecs.firstOrNull()?.apply {
            val icon =
                when (this) {
                    AudioCodec.AC3,
                    AudioCodec.EAC3,
                    AudioCodec.TRUEHD -> CoreR.drawable.ic_dolby
                    else -> null
                }
            VideoMetadataBarItem(text = this.raw, icon = icon)
        }
        videoMetadata.audioChannels.firstOrNull()?.apply { VideoMetadataBarItem(text = this.raw) }
    }
}

@Composable
fun VideoMetadataBarItem(text: String, @DrawableRes icon: Int? = null) {
    Row(
        modifier =
            Modifier.clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(
                    horizontal = MaterialTheme.spacings.small,
                    vertical = MaterialTheme.spacings.extraSmall,
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null)
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
@Preview(showBackground = true)
private fun VideoMetadataBarPreview() {
    FindroidTheme { VideoMetadataBar(videoMetadata = dummyVideoMetadata) }
}
