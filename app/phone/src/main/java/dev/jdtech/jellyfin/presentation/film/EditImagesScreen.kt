package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.editimages.EditImagesState
import dev.jdtech.jellyfin.film.presentation.editimages.EditImagesViewModel
import dev.jdtech.jellyfin.film.presentation.editimages.imageTypeAspectRatio
import dev.jdtech.jellyfin.models.buildItemImageUrl
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.ErrorDialog
import dev.jdtech.jellyfin.presentation.film.components.GridRows
import dev.jdtech.jellyfin.presentation.film.components.MenuActionItem
import dev.jdtech.jellyfin.presentation.film.components.MenuWidth
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageInfo
import org.jellyfin.sdk.model.api.ImageType

/** The types shown together in the editor's "Misc..." section, left to right. */
private val MiscImageTypes = listOf(ImageType.THUMB, ImageType.LOGO, ImageType.ART)

/** One cell in the editor: an existing image, an "add" placeholder, or a busy placeholder. */
private sealed interface ImageSlot {
    val type: ImageType

    data class Existing(override val type: ImageType, val image: ImageInfo) : ImageSlot

    data class Add(override val type: ImageType) : ImageSlot

    data class Busy(override val type: ImageType) : ImageSlot
}

/**
 * Lets an admin view and edit an item's images. Primary is shown on its own, then the Backdrop
 * grid, then a "Misc..." grid with Thumb/Logo/Art. Each grid uses the configured column count for
 * its tile shape (a poster, or a 16:9 thumbnail while the item is an episode). The empty
 * placeholder tile (not an "Add" button) starts the remote provider search.
 */
@Composable
fun EditImagesScreen(
    itemId: UUID,
    navigateBack: () -> Unit,
    navigateToImageSearch: (ImageType, Boolean) -> Unit,
    viewModel: EditImagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(itemId) { viewModel.load(itemId) }

    EditImagesScreenLayout(
        itemId = itemId,
        state = state,
        onEditImage = { type -> navigateToImageSearch(type, state.kind == BaseItemKind.EPISODE) },
        onDelete = { type, index -> viewModel.delete(itemId, type, index) },
        onMakePrimary = { type, index -> viewModel.makePrimary(itemId, type, index) },
        onDismissError = viewModel::dismissError,
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditImagesScreenLayout(
    itemId: UUID,
    state: EditImagesState,
    onEditImage: (ImageType) -> Unit,
    onDelete: (ImageType, Int?) -> Unit,
    onMakePrimary: (ImageType, Int) -> Unit,
    onDismissError: () -> Unit,
    navigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier =
            Modifier.fillMaxSize()
                .recalculateWindowInsets()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreR.string.edit_images)) },
                navigationIcon = {
                    AppIconButton(
                        onClick = navigateBack,
                        variant = AppButtonVariant.Ghost,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_arrow_left),
                                contentDescription = null,
                            )
                        },
                    )
                },
                windowInsets = topBarWindowInsets(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (state.isLoading && state.images.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val isEpisode = state.kind == BaseItemKind.EPISODE
            // An episode's Primary is a 16:9 still; every other item's is a poster.
            val primarySize = if (isEpisode) GridSize.THUMBNAIL else GridSize.POSTER
            val backdropSlots =
                imageSlots(
                    type = ImageType.BACKDROP,
                    images = state.images.filter { it.imageType == ImageType.BACKDROP },
                    downloading = state.downloadingType == ImageType.BACKDROP,
                )
            val backdropBusy = state.busyKey?.startsWith("${ImageType.BACKDROP.name}:") == true

            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = MaterialTheme.spacings.default)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = MaterialTheme.spacings.default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
            ) {
                // Labels sit above each tile, the same way the thumb/logo/art row does.
                GridRows(
                    items = listOf(ImageType.PRIMARY),
                    columns = gridColumns(primarySize),
                ) { type ->
                    SingleTypeTile(
                        type = type,
                        itemId = itemId,
                        state = state,
                        isEpisode = isEpisode,
                        onEditImage = onEditImage,
                        onDelete = onDelete,
                        onMakePrimary = onMakePrimary,
                    )
                }

                GridRows(
                    items = backdropSlots,
                    columns = gridColumns(GridSize.THUMBNAIL),
                ) { slot ->
                    ImageSlotTile(
                        slot = slot,
                        itemId = itemId,
                        baseUrl = state.baseUrl,
                        ratio = imageTypeAspectRatio(ImageType.BACKDROP),
                        enabled = !backdropBusy,
                        // Only the first backdrop names the row; the rest stay label-less.
                        showLabel = slot == backdropSlots.firstOrNull(),
                        onEditImage = onEditImage,
                        onDelete = onDelete,
                        onMakePrimary = onMakePrimary,
                    )
                }

                // No section label here: the Thumb/Logo/Art tiles already carry their own names.
                GridRows(
                    items = MiscImageTypes,
                    columns = gridColumns(GridSize.THUMBNAIL),
                ) { type ->
                    SingleTypeTile(
                        type = type,
                        itemId = itemId,
                        state = state,
                        isEpisode = isEpisode,
                        onEditImage = onEditImage,
                        onDelete = onDelete,
                        onMakePrimary = onMakePrimary,
                    )
                }
            }
        }
    }

    state.error?.let { error -> ErrorDialog(exception = error, onDismissRequest = onDismissError) }
}

/** One type's tile (its image, or the add/busy placeholder), filling its grid cell. */
@Composable
private fun SingleTypeTile(
    type: ImageType,
    itemId: UUID,
    state: EditImagesState,
    isEpisode: Boolean = false,
    showLabel: Boolean = true,
    onEditImage: (ImageType) -> Unit,
    onDelete: (ImageType, Int?) -> Unit,
    onMakePrimary: (ImageType, Int) -> Unit,
) {
    val busy = state.busyKey?.startsWith("${type.name}:") == true
    imageSlots(
            type = type,
            images = state.images.filter { it.imageType == type },
            downloading = state.downloadingType == type,
        )
        .forEach { slot ->
            ImageSlotTile(
                slot = slot,
                itemId = itemId,
                baseUrl = state.baseUrl,
                ratio = imageTypeAspectRatio(type, isEpisode),
                enabled = !busy,
                showLabel = showLabel,
                onEditImage = onEditImage,
                onDelete = onDelete,
                onMakePrimary = onMakePrimary,
            )
        }
}

/**
 * The tiles a [type] contributes. Backdrop (the multi-image type) keeps its images and gains a
 * trailing slot: a busy slot while [downloading], otherwise the add placeholder. A single-image
 * type contributes one slot: a busy placeholder while [downloading], otherwise its image or the add
 * placeholder.
 */
private fun imageSlots(
    type: ImageType,
    images: List<ImageInfo>,
    downloading: Boolean,
): List<ImageSlot> =
    when {
        // Backdrop keeps all of its tiles while a download runs; the busy state shows only in the
        // trailing slot, where the add placeholder normally sits.
        type == ImageType.BACKDROP ->
            images.map { ImageSlot.Existing(type, it) } +
                if (downloading) ImageSlot.Busy(type) else ImageSlot.Add(type)
        // A single-image type has one cell, which becomes the busy placeholder while downloading.
        downloading -> listOf(ImageSlot.Busy(type))
        else ->
            listOf(
                images.firstOrNull()?.let { ImageSlot.Existing(type, it) } ?: ImageSlot.Add(type)
            )
    }

/** One cell: the type label, then the slot's preview or placeholder. */
@Composable
private fun ImageSlotTile(
    slot: ImageSlot,
    itemId: UUID,
    baseUrl: String,
    ratio: Float,
    enabled: Boolean,
    showLabel: Boolean = true,
    onEditImage: (ImageType) -> Unit,
    onDelete: (ImageType, Int?) -> Unit,
    onMakePrimary: (ImageType, Int) -> Unit,
) {
    val label = imageTypeLabel(slot.type)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
    ) {
        // The label row is always present so that tiles with a hidden label stay aligned.
        Text(
            text = if (showLabel) label else "",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        when (slot) {
            is ImageSlot.Existing -> {
                val index = slot.image.imageIndex
                ImagePreview(
                    itemId = itemId,
                    type = slot.type,
                    image = slot.image,
                    baseUrl = baseUrl,
                    ratio = ratio,
                    enabled = enabled,
                    onReplace = { onEditImage(slot.type) },
                    onDelete = { onDelete(slot.type, index) },
                    onMakePrimary = index?.let { { onMakePrimary(slot.type, it) } },
                    makePrimaryEnabled = index != 0,
                )
            }
            is ImageSlot.Add ->
                PlaceholderTile(
                    ratio = ratio,
                    enabled = enabled,
                    onAdd = { onEditImage(slot.type) },
                )
            is ImageSlot.Busy -> BusyTile(ratio = ratio)
        }
    }
}

/**
 * An image preview: the picture, then its resolution and file size and a single overflow button
 * with the image's actions. [onMakePrimary] is only supplied for multi-image types (Backdrop); it
 * is disabled on the image that is already primary.
 */
@Composable
private fun ImagePreview(
    itemId: UUID,
    type: ImageType,
    image: ImageInfo,
    baseUrl: String,
    ratio: Float,
    enabled: Boolean,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    onMakePrimary: (() -> Unit)? = null,
    makePrimaryEnabled: Boolean = true,
) {
    var menuOpen by remember { mutableStateOf(false) }

    AsyncImage(
        model = buildItemImageUrl(baseUrl, itemId, type, image.imageIndex, image.imageTag),
        contentDescription = null,
        contentScale = imageTypeContentScale(type),
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            imageResolution(image)
                .takeIf { it.isNotEmpty() }
                ?.let { resolution ->
                    Text(
                        text = resolution,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            Text(
                text = formatSize(image.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            AppIconButton(
                onClick = { menuOpen = true },
                enabled = enabled,
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_ellipsis),
                        contentDescription = stringResource(CoreR.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.width(MenuWidth),
            ) {
                // A backdrop is multi-image: adding an image appends, so "Replace" is misleading.
                if (type != ImageType.BACKDROP) {
                    MenuActionItem(
                        icon = CoreR.drawable.ic_rotate_ccw,
                        label = stringResource(CoreR.string.replace),
                        enabled = enabled,
                        onClick = {
                            menuOpen = false
                            onReplace()
                        },
                    )
                }
                if (onMakePrimary != null) {
                    MenuActionItem(
                        icon = CoreR.drawable.ic_flame,
                        label = stringResource(CoreR.string.make_primary),
                        enabled = enabled && makePrimaryEnabled,
                        onClick = {
                            menuOpen = false
                            onMakePrimary()
                        },
                    )
                }
                MenuActionItem(
                    icon = CoreR.drawable.ic_trash,
                    label = stringResource(CoreR.string.delete),
                    enabled = enabled,
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/** An empty slot that doubles as the "add" button: tapping it starts the search flow. */
@Composable
private fun PlaceholderTile(
    ratio: Float,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = enabled, onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreR.drawable.ic_plus),
            contentDescription = stringResource(CoreR.string.add),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A tile-sized placeholder with a centred spinner, shown while a remote image is downloading. */
@Composable
private fun BusyTile(ratio: Float) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
    }
}

/** The poster keeps its crop; the wide (16:9) previews are shown whole (contain, no cropping). */
private fun imageTypeContentScale(type: ImageType): ContentScale =
    if (type == ImageType.PRIMARY) ContentScale.Crop else ContentScale.Fit

@Composable
private fun imageTypeLabel(type: ImageType): String =
    when (type) {
        ImageType.PRIMARY -> stringResource(CoreR.string.image_type_primary)
        ImageType.BACKDROP -> stringResource(CoreR.string.image_type_backdrop)
        ImageType.THUMB -> stringResource(CoreR.string.image_type_thumb)
        ImageType.LOGO -> stringResource(CoreR.string.image_type_logo)
        ImageType.ART -> stringResource(CoreR.string.image_type_art)
        else -> type.serialName
    }

private fun imageResolution(image: ImageInfo): String =
    if (image.width != null && image.height != null) "${image.width}×${image.height}" else ""

private fun formatSize(bytes: Long): String =
    when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
