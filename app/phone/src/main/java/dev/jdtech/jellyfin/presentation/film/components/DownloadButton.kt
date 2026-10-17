package dev.jdtech.jellyfin.presentation.film.components

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * The media download action shown in the detail top bar. It reflects the download state: an
 * arrow-down icon to download, a spinner + percentage (and a cancel button) while downloading, and
 * a check when done. Tapping the progress or the check goes to the Downloads page.
 */
@Composable
fun DownloadButton(
    item: FindroidItem,
    downloaderState: DownloaderState?,
    onDownloadClick: (storageIndex: Int) -> Unit,
    onCancelDownloadClick: () -> Unit,
    onDeleteDownloadClick: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var storageSelectionDialogOpen by remember { mutableStateOf(false) }
    var cancelDialogOpen by remember { mutableStateOf(false) }
    var deleteDialogOpen by remember { mutableStateOf(false) }
    var selectedStorageIndex by remember { mutableIntStateOf(0) }
    var storageLocations = remember { context.getExternalFilesDirs(null) }

    val isDownloading = downloaderState?.isDownloading == true

    when {
        isDownloading -> {
            val percent = (downloaderState.progress * 100).toInt().coerceIn(0, 100)
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier.height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable(onClick = onNavigateToDownloads)
                            .padding(horizontal = MaterialTheme.spacings.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(CoreR.string.downloading_at, percent),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                IconButton(
                    onClick = { cancelDialogOpen = true },
                    colors = detailTopBarIconButtonColors(),
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_x),
                        contentDescription = stringResource(CoreR.string.cancel_download),
                    )
                }
            }
        }
        item.isDownloaded() -> {
            IconButton(
                onClick = { deleteDialogOpen = true },
                modifier = modifier,
                colors = detailTopBarIconButtonColors(),
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_trash),
                    contentDescription = stringResource(CoreR.string.delete_download),
                )
            }
        }
        item.canDownload -> {
            IconButton(
                onClick = {
                    storageLocations = context.getExternalFilesDirs(null)
                    if (storageLocations.size > 1) {
                        storageSelectionDialogOpen = true
                    } else {
                        selectedStorageIndex = 0
                        onDownloadClick(selectedStorageIndex)
                    }
                },
                modifier = modifier,
                colors = detailTopBarIconButtonColors(),
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_down_to_line),
                    contentDescription = stringResource(CoreR.string.download),
                )
            }
        }
    }

    if (storageSelectionDialogOpen) {
        val locations = remember {
            storageLocations.map { dir ->
                val locationStringRes =
                    if (Environment.isExternalStorageRemovable(dir)) CoreR.string.external
                    else CoreR.string.internal
                val locationString = context.getString(locationStringRes)

                val stat = StatFs(dir.path)
                val availableMegaBytes = stat.availableBytes.div(1000000)
                context.getString(CoreR.string.storage_name, locationString, availableMegaBytes)
            }
        }
        StorageSelectionDialog(
            storageLocations = locations,
            onSelect = { storageIndex ->
                selectedStorageIndex = storageIndex
                onDownloadClick(selectedStorageIndex)
                storageSelectionDialogOpen = false
            },
            onDismiss = { storageSelectionDialogOpen = false },
        )
    }

    if (cancelDialogOpen) {
        CancelDownloadDialog(
            onCancel = {
                onCancelDownloadClick()
                cancelDialogOpen = false
            },
            onDismiss = { cancelDialogOpen = false },
        )
    }

    if (deleteDialogOpen) {
        DeleteDownloadDialog(
            onDelete = {
                onDeleteDownloadClick()
                deleteDialogOpen = false
            },
            onDismiss = { deleteDialogOpen = false },
        )
    }
}
