package dev.jdtech.jellyfin.presentation.film.components

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.VideoMetadata

/** The optional technical metadata block, styled like the genres/director/writers rows. */
@Composable
fun ExtraInfoText(videoMetadata: VideoMetadata) {
    val context = LocalContext.current
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
