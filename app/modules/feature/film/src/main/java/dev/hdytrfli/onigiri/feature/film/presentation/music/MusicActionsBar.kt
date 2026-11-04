package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppButton(onClick = onPlay, modifier = Modifier.weight(1f)) {
            Icon(painter = painterResource(CoreR.drawable.ic_play), contentDescription = null)
            Text(text = stringResource(CoreR.string.play))
        }
        AppIconButton(
            onClick = onShuffle,
            modifier = Modifier.padding(start = MaterialTheme.spacings.small),
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_shuffle),
                    contentDescription = stringResource(FilmR.string.music_shuffle),
                )
            },
        )
        AppIconButton(
            onClick = onFavorite,
            modifier = Modifier.padding(start = MaterialTheme.spacings.small),
            content = {
                Icon(
                    painter =
                        painterResource(
                            if (favorite) CoreR.drawable.ic_heart_filled
                            else CoreR.drawable.ic_heart
                        ),
                    contentDescription = stringResource(CoreR.string.favorite_button_description),
                    tint = if (favorite) Red else MaterialTheme.colorScheme.onSurface,
                )
            },
        )
        Box(modifier = Modifier.padding(start = MaterialTheme.spacings.small)) {
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
                alignEnd = true,
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
}
