package dev.jdtech.jellyfin.presentation.film.components

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Shows the item's title logo image when available, falling back to plain text otherwise. */
@Composable
fun ItemTitle(
    title: String,
    logoUri: Uri?,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    logoHeight: Dp = 72.dp,
) {
    if (logoUri != null) {
        AsyncImage(
            model = logoUri,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = modifier.fillMaxWidth().height(logoHeight),
        )
    } else {
        Text(
            text = title,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            style = style,
            modifier = modifier,
        )
    }
}
