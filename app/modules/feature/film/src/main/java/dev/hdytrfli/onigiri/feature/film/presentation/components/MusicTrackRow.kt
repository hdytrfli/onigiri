package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppDropdownMenu
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.MenuActionItem
import dev.hdytrfli.onigiri.ui.theme.spacings

/** One action in a [MusicTrackRow]'s dropdown menu. */
data class MusicRowAction(
    val label: String,
    @DrawableRes val icon: Int,
    val enabled: Boolean = true,
    val iconTint: Color? = null,
    val dismissOnClick: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * The shared song row used by the music library and the playback queue, so both lists keep the same
 * rounded container, artwork, current-track indicator and dropdown behaviour.
 */
@Composable
fun MusicTrackRow(
    title: String,
    subtitle: String,
    imageUri: Any?,
    isCurrent: Boolean,
    actions: List<MusicRowAction>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragging: Boolean = false,
    trailingText: String? = null,
    trailingDivider: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh
                    else Color.Transparent
                )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .height(72.dp)
                    .clickable(onClick = onClick)
                    .padding(horizontal = MaterialTheme.spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isCurrent) {
                    Box(
                        modifier =
                            Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f))
                    )
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_play),
                        contentDescription = stringResource(FilmR.string.music_now_playing),
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = MaterialTheme.spacings.small)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            trailingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = MaterialTheme.spacings.extraSmall),
                )
            }

            if (actions.isNotEmpty()) {
                Box {
                    AppIconButton(
                        onClick = { menuExpanded = true },
                        variant = AppButtonVariant.Ghost,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_ellipsis),
                                contentDescription = stringResource(CoreR.string.more_options),
                            )
                        },
                    )
                    AppDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        actions.forEach { action ->
                            MenuActionItem(
                                label = action.label,
                                icon = action.icon,
                                enabled = action.enabled,
                                iconTint = action.iconTint,
                                onClick = {
                                    if (action.dismissOnClick) {
                                        menuExpanded = false
                                    }
                                    action.onClick()
                                },
                            )
                        }
                    }
                }
            }
        }

        if (trailingDivider) {
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.05f))
        }
    }
}
