package dev.hdytrfli.onigiri.feature.film.presentation.components

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderState
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.isDownloaded
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonGroup
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator

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
    downloadAvailable: Boolean = item.canDownload,
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
            AppButtonGroup(modifier = modifier) {
                AppButton(
                    onClick = onNavigateToDownloads,
                    variant = AppButtonVariant.Secondary,
                    content = {
                        AppLoadingIndicator(modifier = Modifier.size(20.dp))
                        Text(text = stringResource(CoreR.string.downloading_at, percent))
                    },
                )
                AppIconButton(
                    onClick = { cancelDialogOpen = true },
                    variant = AppButtonVariant.Secondary,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_x),
                            contentDescription = stringResource(CoreR.string.cancel_download),
                        )
                    },
                )
            }
        }
        item.isDownloaded() -> {
            AppIconButton(
                onClick = { deleteDialogOpen = true },
                modifier = modifier,
                variant = AppButtonVariant.Secondary,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_trash),
                        contentDescription = stringResource(CoreR.string.delete_download),
                    )
                },
            )
        }
        downloadAvailable -> {
            AppIconButton(
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
                variant = AppButtonVariant.Secondary,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_arrow_down_to_line),
                        contentDescription = stringResource(CoreR.string.download),
                    )
                },
            )
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
