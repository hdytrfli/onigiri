package dev.jdtech.jellyfin.presentation.film.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.theme.Red
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.AppIconButton
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
    playLabel: String? = null,
    @DrawableRes playIconRes: Int = CoreR.drawable.ic_play,
    showRestartButton: Boolean = true,
    hasProgress: Boolean = item.playbackPositionTicks > 0,
    forcePlayEnabled: Boolean = false,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        if (
            !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                PlayButton(
                    item = item,
                    onClick = { onPlayClick(false) },
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    enabled = (item.canPlay || forcePlayEnabled) && canPlay,
                    label = playLabel,
                    iconRes = playIconRes,
                )
                if (showRestartButton && hasProgress) {
                    AppIconButton(
                        onClick = { onPlayClick(true) },
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = null,
                            )
                        },
                    )
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
                    enabled = (item.canPlay || forcePlayEnabled) && canPlay,
                    label = playLabel,
                    iconRes = playIconRes,
                )
                if (showRestartButton && hasProgress) {
                    AppIconButton(
                        onClick = { onPlayClick(true) },
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            AppIconButton(
                onClick = onMarkAsPlayedClick,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_check),
                        contentDescription = null,
                        tint = if (item.played) Red else LocalContentColor.current,
                    )
                },
            )
            AppIconButton(
                onClick = onMarkAsFavoriteClick,
                content = {
                    when (item.favorite) {
                        true ->
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_heart_filled),
                                contentDescription = null,
                                tint = Red,
                            )
                        false ->
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_heart),
                                contentDescription = null,
                            )
                    }
                },
            )
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
