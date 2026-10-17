package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.Dimens
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalGridSettings
import dev.jdtech.jellyfin.presentation.utils.MAX_GRID_COLUMNS
import dev.jdtech.jellyfin.presentation.utils.MIN_GRID_COLUMNS
import dev.jdtech.jellyfin.settings.domain.GridId

/**
 * How [GridSettingsButton] is drawn, so it blends in wherever it sits: [ICON] for a `TopAppBar` (or
 * the search bar), [FILLED_TONAL] for a detail-page top bar that also hosts filled icon actions,
 * and [SURFACE] for the circular action cluster in [HomeHeader].
 */
enum class GridSettingsButtonStyle {
    ICON,
    FILLED_TONAL,
    SURFACE,
}

/**
 * An icon button that opens [GridSettingsDialog] for [gridId]. It owns the dialog visibility, so it
 * can be dropped into any top bar, header or title row. [style] lets the caller match the
 * neighbours at the call site.
 */
@Composable
fun GridSettingsButton(
    gridId: GridId,
    modifier: Modifier = Modifier,
    style: GridSettingsButtonStyle = GridSettingsButtonStyle.ICON,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    val icon: @Composable () -> Unit = {
        Icon(
            painter = painterResource(CoreR.drawable.ic_layout_grid),
            contentDescription = stringResource(CoreR.string.grid_layout),
        )
    }

    when (style) {
        GridSettingsButtonStyle.ICON ->
            IconButton(onClick = { showDialog = true }, modifier = modifier) { icon() }
        GridSettingsButtonStyle.FILLED_TONAL ->
            FilledTonalIconButton(onClick = { showDialog = true }, modifier = modifier) { icon() }
        GridSettingsButtonStyle.SURFACE ->
            Surface(
                onClick = { showDialog = true },
                modifier = modifier.fillMaxHeight().aspectRatio(1f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
    }

    if (showDialog) {
        GridSettingsDialog(gridId = gridId, onDismiss = { showDialog = false })
    }
}

/**
 * The column count settings for [gridId], with a separate control for portrait and landscape.
 * Changes apply and persist immediately.
 */
@Composable
fun GridSettingsDialog(gridId: GridId, onDismiss: () -> Unit) {
    val gridSettings = LocalGridSettings.current

    var portrait by remember { mutableIntStateOf(gridSettings.columnCount(gridId, false)) }
    var landscape by remember { mutableIntStateOf(gridSettings.columnCount(gridId, true)) }

    BaseDialog(
        title = stringResource(CoreR.string.grid_layout),
        onDismiss = onDismiss,
        titleStyle = MaterialTheme.typography.titleMedium,
        padding = MaterialTheme.spacings.medium,
        maxWidth = Dimens.dialogMaxWidth,
    ) { contentPadding ->
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(contentPadding)
                    .padding(bottom = MaterialTheme.spacings.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
        ) {
            ColumnCountSetting(
                label = stringResource(CoreR.string.portrait),
                value = portrait,
                onValueChange = {
                    portrait = it
                    gridSettings.onColumnsChange(gridId, it, landscape)
                },
            )
            ColumnCountSetting(
                label = stringResource(CoreR.string.landscape),
                value = landscape,
                onValueChange = {
                    landscape = it
                    gridSettings.onColumnsChange(gridId, portrait, it)
                },
            )
        }
    }
}

@Composable
private fun ColumnCountSetting(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValueChange(value - 1) },
                enabled = value > MIN_GRID_COLUMNS,
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_minus_fat),
                    contentDescription = null,
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 32.dp),
            )
            IconButton(
                onClick = { onValueChange(value + 1) },
                enabled = value < MAX_GRID_COLUMNS,
            ) {
                Icon(painter = painterResource(CoreR.drawable.ic_plus), contentDescription = null)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GridSettingsDialogPreview() {
    FindroidTheme { GridSettingsDialog(gridId = GridId.POSTERS, onDismiss = {}) }
}
