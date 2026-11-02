package dev.hdytrfli.onigiri.feature.film.presentation.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.models.VideoMetadata
import dev.hdytrfli.onigiri.ui.theme.spacings

/** The optional technical metadata block, with the same section-title style as Cast & Crew. */
@Composable
fun ExtraInfoText(videoMetadata: VideoMetadata) {
    val context = LocalContext.current
    Column {
        Text(
            text = stringResource(CoreR.string.media_detail),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        InfoRows(
            rows =
                listOfNotNull(
                    stringResource(CoreR.string.size) to
                        Formatter.formatFileSize(context, videoMetadata.size),
                    if (videoMetadata.videoTracks.isNotEmpty()) {
                        stringResource(CoreR.string.video) to
                            videoMetadata.videoTracks.joinToString { it }
                    } else {
                        null
                    },
                    if (videoMetadata.audioTracks.isNotEmpty()) {
                        stringResource(CoreR.string.audio) to
                            videoMetadata.audioTracks.joinToString { it }
                    } else {
                        null
                    },
                    if (videoMetadata.subtitleTracks.isNotEmpty()) {
                        stringResource(CoreR.string.subtitle) to
                            videoMetadata.subtitleTracks.joinToString { it }
                    } else {
                        null
                    },
                )
        )
    }
}
