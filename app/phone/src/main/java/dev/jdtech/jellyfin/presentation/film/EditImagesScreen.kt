package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.editimages.EditImagesState
import dev.jdtech.jellyfin.film.presentation.editimages.EditImagesViewModel
import dev.jdtech.jellyfin.film.presentation.editimages.imageTypeAspectRatio
import dev.jdtech.jellyfin.models.buildItemImageUrl
import dev.jdtech.jellyfin.presentation.components.ErrorDialog
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageInfo
import org.jellyfin.sdk.model.api.ImageType

/** The display order for the image types, most important first. */
private val ImageTypeOrder =
    listOf(
        ImageType.PRIMARY,
        ImageType.BACKDROP,
        ImageType.THUMB,
        ImageType.LOGO,
        ImageType.BANNER,
        ImageType.ART,
        ImageType.DISC,
        ImageType.BOX,
        ImageType.BOX_REAR,
        ImageType.MENU,
        ImageType.SCREENSHOT,
        ImageType.PROFILE,
    )

/** Shared width for every 16:9 tile, so all 16:9 previews line up. */
private val WideImageTileWidth = 280.dp

/** Width of the per-tile overflow menu. */
private val OverflowMenuWidth = 220.dp

/**
 * Lets an admin view and edit an item's images, grouped by type: replace an image, delete one, and
 * add images to a type by searching the server's remote providers. Full-width, with each type
 * rendered as a plain section (no cards); the empty placeholder tile starts the search flow.
 */
@Composable
fun EditImagesScreen(
    itemId: UUID,
    navigateBack: () -> Unit,
    navigateToImageSearch: (ImageType) -> Unit,
    viewModel: EditImagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(itemId) { viewModel.load(itemId) }

    EditImagesScreenLayout(
        itemId = itemId,
        state = state,
        onEditImage = navigateToImageSearch,
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
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
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
            // Types relevant to the item, plus any other type it already has, sorted by
            // importance. CHAPTER is server-generated from the video, so it is never editable.
            val presentTypes =
                state.images.map { it.imageType }.distinct().filter { it != ImageType.CHAPTER }
            val types =
                (defaultImageTypes(state.kind) + presentTypes).distinct().sortedBy {
                    imageTypeSortIndex(it)
                }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
                contentPadding = PaddingValues(all = MaterialTheme.spacings.default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
            ) {
                items(items = types, key = { it.name }) { type ->
                    ImageTypeSection(
                        itemId = itemId,
                        type = type,
                        images = state.images.filter { it.imageType == type },
                        baseUrl = state.baseUrl,
                        busy = state.busyKey?.startsWith("${type.name}:") == true,
                        downloading = state.downloadingType == type,
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

/** A plain, full-width section for one image [type]: a header, then the type's tiles beneath it. */
@Composable
private fun ImageTypeSection(
    itemId: UUID,
    type: ImageType,
    images: List<ImageInfo>,
    baseUrl: String,
    busy: Boolean,
    downloading: Boolean,
    onEditImage: (ImageType) -> Unit,
    onDelete: (ImageType, Int?) -> Unit,
    onMakePrimary: (ImageType, Int) -> Unit,
) {
    val ratio = imageTypeAspectRatio(type)
    val tileWidth = imageTileWidth(type)

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
        Text(text = imageTypeLabel(type), style = MaterialTheme.typography.titleMedium)
        when {
            // While a remote download runs, show a single centred placeholder for the type.
            downloading -> BusyTile(width = tileWidth, ratio = ratio)
            // Backdrop is the only multi-image type: its tiles plus a trailing add placeholder.
            type == ImageType.BACKDROP ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)
                ) {
                    items(items = images, key = { it.imageIndex ?: it.hashCode() }) { image ->
                        val index = image.imageIndex
                        ImageTile(
                            itemId = itemId,
                            type = type,
                            image = image,
                            baseUrl = baseUrl,
                            width = tileWidth,
                            ratio = ratio,
                            enabled = !busy,
                            onReplace = { onEditImage(type) },
                            onDelete = { onDelete(type, index) },
                            onMakePrimary = index?.let { { onMakePrimary(type, it) } },
                            makePrimaryEnabled = index != 0,
                        )
                    }
                    item(key = "add") {
                        PlaceholderTile(
                            width = tileWidth,
                            ratio = ratio,
                            enabled = !busy,
                            onAdd = { onEditImage(type) },
                        )
                    }
                }
            else -> {
                val image = images.firstOrNull()
                if (image == null) {
                    PlaceholderTile(
                        width = tileWidth,
                        ratio = ratio,
                        enabled = !busy,
                        onAdd = { onEditImage(type) },
                    )
                } else {
                    ImageTile(
                        itemId = itemId,
                        type = type,
                        image = image,
                        baseUrl = baseUrl,
                        width = tileWidth,
                        ratio = ratio,
                        enabled = !busy,
                        onReplace = { onEditImage(type) },
                        onDelete = { onDelete(type, image.imageIndex) },
                    )
                }
            }
        }
    }
}

/**
 * An image tile: the preview, then its resolution and file size and a single overflow button with
 * the tile's actions. [onMakePrimary] is only supplied for multi-image types (Backdrop); it is
 * disabled on the image that is already primary.
 */
@Composable
private fun ImageTile(
    itemId: UUID,
    type: ImageType,
    image: ImageInfo,
    baseUrl: String,
    width: Dp,
    ratio: Float,
    enabled: Boolean,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    onMakePrimary: (() -> Unit)? = null,
    makePrimaryEnabled: Boolean = true,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
    ) {
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
                IconButton(onClick = { menuOpen = true }, enabled = enabled) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_ellipsis),
                        contentDescription = stringResource(CoreR.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.width(OverflowMenuWidth),
                ) {
                    // Header: the image type name, muted and not clickable.
                    DropdownMenuItem(
                        text = { Text(imageTypeLabel(type)) },
                        enabled = false,
                        onClick = {},
                    )
                    MenuActionItem(
                        icon = CoreR.drawable.ic_rotate_ccw,
                        label = stringResource(CoreR.string.replace),
                        enabled = enabled,
                        onClick = {
                            menuOpen = false
                            onReplace()
                        },
                    )
                    MenuActionItem(
                        icon = CoreR.drawable.ic_trash,
                        label = stringResource(CoreR.string.delete),
                        enabled = enabled,
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                    if (onMakePrimary != null) {
                        MenuActionItem(
                            icon = CoreR.drawable.ic_star,
                            label = stringResource(CoreR.string.make_primary),
                            enabled = enabled && makePrimaryEnabled,
                            onClick = {
                                menuOpen = false
                                onMakePrimary()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** A single action row in the per-tile overflow menu, with a leading icon. */
@Composable
private fun MenuActionItem(icon: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(painter = painterResource(icon), contentDescription = null) },
        enabled = enabled,
        onClick = onClick,
    )
}

/** An empty slot that doubles as the "add" button: tapping it starts the search flow. */
@Composable
private fun PlaceholderTile(
    width: Dp,
    ratio: Float,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    Box(
        modifier =
            Modifier.width(width)
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
private fun BusyTile(width: Dp, ratio: Float) {
    Box(
        modifier =
            Modifier.width(width)
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
    }
}

/**
 * The tile width for [type]: the poster/square types are 160dp, and every 16:9 type (including
 * logo, banner and backdrop) shares one width so they line up.
 */
private fun imageTileWidth(type: ImageType): Dp =
    when (type) {
        ImageType.PRIMARY,
        ImageType.DISC,
        ImageType.BOX -> 160.dp
        else -> WideImageTileWidth
    }

/** 16:9 previews are shown whole (Fit); the poster and square types fill their container (Crop). */
private fun imageTypeContentScale(type: ImageType): ContentScale =
    when (type) {
        ImageType.PRIMARY,
        ImageType.DISC,
        ImageType.BOX -> ContentScale.Crop
        else -> ContentScale.Fit
    }

/** The position of [type] in the importance order; unknown types sort last. */
private fun imageTypeSortIndex(type: ImageType): Int =
    ImageTypeOrder.indexOf(type).let { if (it >= 0) it else ImageTypeOrder.size }

/** The image types offered for an item kind, before adding any type the item already has. */
private fun defaultImageTypes(kind: BaseItemKind?): List<ImageType> =
    when (kind) {
        BaseItemKind.MOVIE ->
            listOf(
                ImageType.ART,
                ImageType.BACKDROP,
                ImageType.BANNER,
                ImageType.BOX,
                ImageType.BOX_REAR,
                ImageType.DISC,
                ImageType.LOGO,
                ImageType.MENU,
                ImageType.PRIMARY,
                ImageType.SCREENSHOT,
                ImageType.THUMB,
            )
        BaseItemKind.SERIES,
        BaseItemKind.SEASON ->
            listOf(
                ImageType.BACKDROP,
                ImageType.BANNER,
                ImageType.LOGO,
                ImageType.PRIMARY,
                ImageType.THUMB,
                ImageType.ART,
            )
        BaseItemKind.EPISODE -> listOf(ImageType.PRIMARY, ImageType.THUMB, ImageType.BACKDROP)
        else -> listOf(ImageType.PRIMARY, ImageType.BACKDROP, ImageType.LOGO, ImageType.THUMB)
    }

@Composable
private fun imageTypeLabel(type: ImageType): String =
    when (type) {
        ImageType.PRIMARY -> stringResource(CoreR.string.image_type_primary)
        ImageType.ART -> stringResource(CoreR.string.image_type_art)
        ImageType.BACKDROP -> stringResource(CoreR.string.image_type_backdrop)
        ImageType.BANNER -> stringResource(CoreR.string.image_type_banner)
        ImageType.LOGO -> stringResource(CoreR.string.image_type_logo)
        ImageType.THUMB -> stringResource(CoreR.string.image_type_thumb)
        ImageType.DISC -> stringResource(CoreR.string.image_type_disc)
        ImageType.BOX -> stringResource(CoreR.string.image_type_box)
        ImageType.BOX_REAR -> stringResource(CoreR.string.image_type_box_rear)
        ImageType.SCREENSHOT -> stringResource(CoreR.string.image_type_screenshot)
        ImageType.MENU -> stringResource(CoreR.string.image_type_menu)
        ImageType.PROFILE -> stringResource(CoreR.string.image_type_profile)
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
