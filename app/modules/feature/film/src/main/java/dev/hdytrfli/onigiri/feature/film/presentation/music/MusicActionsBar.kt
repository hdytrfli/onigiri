package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.window.core.layout.WindowSizeClass
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppDropdownMenu
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.MenuActionItem
import dev.hdytrfli.onigiri.ui.theme.Red
import dev.hdytrfli.onigiri.ui.theme.spacings

/** Play/shuffle/favorite plus the less common queue actions for an artist or album. */
@Composable
fun MusicActionsBar(
    favorite: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact =
        !currentWindowAdaptiveInfo()
            .windowSizeClass
            .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                AppButton(onClick = onPlay, modifier = Modifier.weight(1f)) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_play),
                        contentDescription = null,
                    )
                    Text(text = stringResource(CoreR.string.play))
                }
                ShuffleButton(onShuffle = onShuffle)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                FavoriteButton(favorite = favorite, onFavorite = onFavorite)
                QueueMenuButton(onPlayNext = onPlayNext, onAddToQueue = onAddToQueue)
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            AppButton(onClick = onPlay) {
                Icon(painter = painterResource(CoreR.drawable.ic_play), contentDescription = null)
                Text(text = stringResource(CoreR.string.play))
            }
            ShuffleButton(onShuffle = onShuffle)
            FavoriteButton(favorite = favorite, onFavorite = onFavorite)
            QueueMenuButton(onPlayNext = onPlayNext, onAddToQueue = onAddToQueue)
        }
    }
}

@Composable
private fun ShuffleButton(onShuffle: () -> Unit) {
    AppIconButton(
        onClick = onShuffle,
        content = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_shuffle),
                contentDescription = stringResource(FilmR.string.music_shuffle),
            )
        },
    )
}

@Composable
private fun FavoriteButton(favorite: Boolean, onFavorite: () -> Unit) {
    AppIconButton(
        onClick = onFavorite,
        content = {
            Icon(
                painter =
                    painterResource(
                        if (favorite) CoreR.drawable.ic_heart_filled else CoreR.drawable.ic_heart
                    ),
                contentDescription = stringResource(CoreR.string.favorite_button_description),
                tint = if (favorite) Red else MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

@Composable
private fun QueueMenuButton(onPlayNext: () -> Unit, onAddToQueue: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        AppIconButton(
            onClick = { menuExpanded = true },
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
            MenuActionItem(
                label = stringResource(FilmR.string.music_play_next),
                icon = CoreR.drawable.ic_skip_forward,
                onClick = {
                    menuExpanded = false
                    onPlayNext()
                },
            )
            MenuActionItem(
                label = stringResource(FilmR.string.music_add_to_queue),
                icon = CoreR.drawable.ic_plus,
                onClick = {
                    menuExpanded = false
                    onAddToQueue()
                },
            )
        }
    }
}
