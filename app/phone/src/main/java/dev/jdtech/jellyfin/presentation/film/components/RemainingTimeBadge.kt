package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.core.presentation.theme.OnScrim
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

/** A poster badge showing how many minutes are left: white text on opaque black. */
@Composable
fun RemainingTimeBadge(item: FindroidItem, modifier: Modifier = Modifier) {
    val minutesLeft =
        ((item.runtimeTicks - item.playbackPositionTicks) / 600000000).coerceAtLeast(0)
    Box(
        modifier =
            modifier
                .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.scrim),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(CoreR.string.runtime_minutes_left, minutesLeft),
            color = OnScrim,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacings.extraSmall),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RemainingTimeBadgePreview() {
    FindroidTheme { RemainingTimeBadge(item = dummyMovie) }
}
