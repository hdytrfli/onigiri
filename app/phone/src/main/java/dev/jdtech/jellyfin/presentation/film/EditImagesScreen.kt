package dev.jdtech.jellyfin.presentation.film

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageInfo
import org.jellyfin.sdk.model.api.ImageType

/** Which image slot a pending add/replace belongs to; a null index means a single-image type. */
private data class ImageTarget(val type: ImageType, val index: Int?)

/**
 * Lets an admin view and edit an item's images, grouped by type: upload or replace an image, delete
 * one, and add images to a type. Full-width, with each type rendered as a plain section (no cards);
 * the empty placeholder tile is what starts the add flow.
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingUpload by remember { mutableStateOf<ImageTarget?>(null) }
    var chooserTarget by remember { mutableStateOf<ImageTarget?>(null) }

    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val target = pendingUpload
            pendingUpload = null
            if (uri == null || target == null) return@rememberLauncherForActivityResult
            scope.launch {
                val bytes =
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                if (bytes != null) {
                    val mediaType = context.contentResolver.getType(uri) ?: "image/*"
                    viewModel.upload(
                        itemId = itemId,
                        type = target.type,
                        index = target.index,
                        content = bytes,
                        mediaType = mediaType,
                    )
                }
            }
        }

    EditImagesScreenLayout(
        itemId = itemId,
        state = state,
        onAdd = { type, index -> chooserTarget = ImageTarget(type, index) },
        onReplace = { type, index -> chooserTarget = ImageTarget(type, index) },
        onDelete = { type, index -> viewModel.delete(itemId, type, index) },
        onMakePrimary = { type, index -> viewModel.makePrimary(itemId, type, index) },
        onDismissError = viewModel::dismissError,
        navigateBack = navigateBack,
    )

    chooserTarget?.let { target ->
        ImageSourceChooser(
            onUpload = {
                chooserTarget = null
                pendingUpload = target
                filePicker.launch("image/*")
            },
            onSearch = {
                chooserTarget = null
                navigateToImageSearch(target.type)
            },
            onDismiss = { chooserTarget = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditImagesScreenLayout(
    itemId: UUID,
    state: EditImagesState,
    onAdd: (ImageType, Int?) -> Unit,
    onReplace: (ImageType, Int?) -> Unit,
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
            // Types relevant to the item, followed by any other type the item already has. CHAPTER
            // is server-generated from the video, so it is never editable.
            val presentTypes =
                state.images.map { it.imageType }.distinct().filter { it != ImageType.CHAPTER }
            val types = (defaultImageTypes(state.kind) + presentTypes).distinct()

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
                        onAdd = onAdd,
                        onReplace = onReplace,
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
    onAdd: (ImageType, Int?) -> Unit,
    onReplace: (ImageType, Int?) -> Unit,
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
                            onMakePrimary = index?.let { { onMakePrimary(type, it) } },
                            makePrimaryEnabled = index != 0,
                            onReplace = { onReplace(type, index) },
                            onDelete = { onDelete(type, index) },
                        )
                    }
                    item(key = "add") {
                        PlaceholderTile(
                            width = tileWidth,
                            ratio = ratio,
                            enabled = !busy,
                            onAdd = { onAdd(type, images.size) },
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
                        onAdd = { onAdd(type, null) },
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
                        onReplace = { onReplace(type, image.imageIndex) },
                        onDelete = { onDelete(type, image.imageIndex) },
                    )
                }
            }
        }
    }
}

/**
 * An image tile: the preview, then its resolution/size and the actions. [onMakePrimary] is only
 * supplied for multi-image types (Backdrop); it is disabled on the image that is already primary.
 */
@Composable
private fun ImageTile(
    itemId: UUID,
    type: ImageType,
    image: ImageInfo,
    baseUrl: String,
    width: Dp?,
    ratio: Float,
    enabled: Boolean,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    onMakePrimary: (() -> Unit)? = null,
    makePrimaryEnabled: Boolean = true,
) {
    Column(
        modifier = Modifier.tileWidth(width),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
    ) {
        AsyncImage(
            model = buildItemImageUrl(baseUrl, itemId, type, image.imageIndex, image.imageTag),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = imageMetadata(image),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onMakePrimary != null) {
                IconButton(onClick = onMakePrimary, enabled = enabled && makePrimaryEnabled) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_star),
                        contentDescription = stringResource(CoreR.string.make_primary),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onReplace, enabled = enabled) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                    contentDescription = stringResource(CoreR.string.replace),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_trash),
                    contentDescription = stringResource(CoreR.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** An empty slot that doubles as the "add" button: tapping it starts the add flow. */
@Composable
private fun PlaceholderTile(
    width: Dp?,
    ratio: Float,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    Box(
        modifier =
            Modifier.tileWidth(width)
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
private fun BusyTile(width: Dp?, ratio: Float) {
    Box(
        modifier =
            Modifier.tileWidth(width)
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
    }
}

/** Applies the tile's fixed [width], or the full available width when it is null (logo/banner). */
private fun Modifier.tileWidth(width: Dp?): Modifier =
    if (width == null) this.fillMaxWidth() else this.width(width)

/**
 * The tile width for [type]; null means "full width" (used for the wide logo/banner shapes). Sizes
 * are deliberately generous: too big reads better than too small.
 */
private fun imageTileWidth(type: ImageType): Dp? =
    when (type) {
        ImageType.PRIMARY,
        ImageType.DISC,
        ImageType.BOX,
        ImageType.BOX_REAR,
        ImageType.PROFILE -> 160.dp
        ImageType.LOGO,
        ImageType.BANNER -> null
        else -> 220.dp
    }

/**
 * The chooser shown when adding or replacing an image: upload from the device or search remotely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSourceChooser(
    onUpload: () -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.spacings.default)
        ) {
            ChooserRow(
                icon = CoreR.drawable.ic_image,
                label = stringResource(CoreR.string.upload),
                onClick = onUpload,
            )
            ChooserRow(
                icon = CoreR.drawable.ic_search,
                label = stringResource(CoreR.string.search),
                onClick = onSearch,
            )
        }
    }
}

@Composable
private fun ChooserRow(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = MaterialTheme.spacings.default,
                    vertical = MaterialTheme.spacings.medium,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(MaterialTheme.spacings.medium))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

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

private fun imageMetadata(image: ImageInfo): String {
    val dimensions =
        if (image.width != null && image.height != null) "${image.width}×${image.height}" else null
    return listOfNotNull(dimensions, formatSize(image.size)).joinToString(" · ")
}

private fun formatSize(bytes: Long): String =
    when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
