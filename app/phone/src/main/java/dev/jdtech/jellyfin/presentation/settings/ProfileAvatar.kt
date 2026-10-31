package dev.jdtech.jellyfin.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    accessToken: String?,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val context = LocalContext.current
    var failed by remember(avatarUrl) { mutableStateOf(false) }
    val request =
        remember(avatarUrl, accessToken) {
            avatarUrl?.let { url ->
                ImageRequest.Builder(context)
                    .data(url)
                    .apply {
                        accessToken?.let { token ->
                            httpHeaders(NetworkHeaders.Builder().set("X-Emby-Token", token).build())
                        }
                    }
                    .build()
            }
        }

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreR.drawable.ic_user),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        if (request != null && !failed) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

@Composable
fun ProfileRoleBadge(isAdmin: Boolean, modifier: Modifier = Modifier) {
    val color =
        if (isAdmin) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter =
                    painterResource(
                        if (isAdmin) CoreR.drawable.ic_shield else CoreR.drawable.ic_user
                    ),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text =
                    stringResource(if (isAdmin) R.string.profile_admin else R.string.profile_guest),
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}
