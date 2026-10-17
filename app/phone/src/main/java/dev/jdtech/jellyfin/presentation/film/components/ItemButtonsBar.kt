package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.theme.Red
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.TooltipFilledTonalIconButton
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun ItemButtonsBar(
    item: FindroidItem,
    onPlayClick: (startFromBeginning: Boolean) -> Unit,
    onMarkAsPlayedClick: () -> Unit,
    onMarkAsFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    canPlay: Boolean = true,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            if (
                !windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                )
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                    PlayButton(
                        item = item,
                        onClick = { onPlayClick(false) },
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        enabled = item.canPlay && canPlay,
                    )
                    if (item.playbackPositionTicks.div(600000000) > 0) {
                        TooltipFilledTonalIconButton(
                            label = stringResource(CoreR.string.start_from_beginning),
                            onClick = { onPlayClick(true) },
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_step_back),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                if (
                    windowSizeClass.isWidthAtLeastBreakpoint(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                    )
                ) {
                    PlayButton(
                        item = item,
                        onClick = { onPlayClick(false) },
                        enabled = item.canPlay && canPlay,
                    )
                    if (item.playbackPositionTicks.div(600000000) > 0) {
                        TooltipFilledTonalIconButton(
                            label = stringResource(CoreR.string.start_from_beginning),
                            onClick = { onPlayClick(true) },
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_step_back),
                                contentDescription = null,
                            )
                        }
                    }
                }
                TooltipFilledTonalIconButton(
                    label =
                        stringResource(
                            if (item.played) CoreR.string.unmark_as_played
                            else CoreR.string.mark_as_played
                        ),
                    onClick = onMarkAsPlayedClick,
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_check),
                        contentDescription = null,
                        tint = if (item.played) Red else LocalContentColor.current,
                    )
                }
                TooltipFilledTonalIconButton(
                    label =
                        stringResource(
                            if (item.favorite) CoreR.string.remove_from_favorites
                            else CoreR.string.add_to_favorites
                        ),
                    onClick = onMarkAsFavoriteClick,
                ) {
                    when (item.favorite) {
                        true -> {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_heart_filled),
                                contentDescription = null,
                                tint = Red,
                            )
                        }
                        false -> {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_heart),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemButtonsBarPreview() {
    FindroidTheme {
        ItemButtonsBar(
            item = dummyEpisode,
            onPlayClick = {},
            onMarkAsPlayedClick = {},
            onMarkAsFavoriteClick = {},
        )
    }
}
