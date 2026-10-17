package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

/** A poster badge showing how many minutes are left, for a partially watched item. */
@Composable
fun RemainingTimeBadge(item: FindroidItem, modifier: Modifier = Modifier) {
    val minutesLeft =
        ((item.runtimeTicks - item.playbackPositionTicks) / 600000000).coerceAtLeast(0)
    BaseBadge(modifier = modifier) {
        Text(
            text = stringResource(CoreR.string.runtime_minutes_left, minutesLeft),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.align(Alignment.Center)
                    .padding(horizontal = MaterialTheme.spacings.extraSmall),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RemainingTimeBadgePreview() {
    FindroidTheme { RemainingTimeBadge(item = dummyMovie) }
}
