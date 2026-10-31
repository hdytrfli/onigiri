package dev.jdtech.jellyfin.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    accessToken: String?,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val context = LocalContext.current
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
    val request = avatarUrl?.let { url ->
        ImageRequest.Builder(context)
            .data(url)
            .apply {
                accessToken?.let { token ->
                    httpHeaders(NetworkHeaders.Builder().set("X-Emby-Token", token).build())
                }
            }
            .build()
    }

    Box(
        modifier = modifier.size(size).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholder,
                error = painterResource(CoreR.drawable.ic_user),
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else {
            Icon(
                painter = painterResource(CoreR.drawable.ic_user),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.58f),
            )
        }
    }
}
