package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.ui.theme.OnScrim
import dev.hdytrfli.onigiri.ui.theme.spacings

/**
 * The age rating drawn over a poster, using the same treatment as the time-left badge: opaque
 * black, white label, rounded to the app's corner radius. A short value like "12" still reads as a
 * pill because of the horizontal padding.
 */
@Composable
fun AgeRatingBadge(rating: String, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.scrim),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rating,
            color = OnScrim,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacings.small),
        )
    }
}
