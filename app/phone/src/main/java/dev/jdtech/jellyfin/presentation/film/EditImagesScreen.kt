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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.editimages.EditImagesState
import dev.jdtech.jellyfin.film.presentation.editimages.EditImagesViewModel
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

private val ThumbnailSize = 96.dp

/** Which image slot a pending file-picker result belongs to. */
private data class PendingUpload(val type: ImageType, val index: Int?)

/**
 * Lets an admin view and edit an item's images, grouped by type: upload or replace an image, delete
 * one, and (for backdrops) promote an image to primary. Styled like the settings pages.
 */
@Composable
fun EditImagesScreen(
    itemId: UUID,
    navigateBack: () -> Unit,
    viewModel: EditImagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(itemId) { viewModel.load(itemId) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingUpload by remember { mutableStateOf<PendingUpload?>(null) }

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
        onUpload = { type, index ->
            pendingUpload = PendingUpload(type, index)
            filePicker.launch("image/*")
        },
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
    onUpload: (ImageType, Int?) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(items = types, key = { it.name }) { type ->
                    val images = state.images.filter { it.imageType == type }
                    ImageTypeCard(
                        itemId = itemId,
                        type = type,
                        images = images,
                        baseUrl = state.baseUrl,
                        busy = state.busyKey?.startsWith("${type.name}:") == true,
                        onUpload = onUpload,
                        onDelete = onDelete,
                        onMakePrimary = onMakePrimary,
                    )
                }
            }
        }
    }

    state.error?.let { error -> ErrorDialog(exception = error, onDismissRequest = onDismissError) }
}

@Composable
private fun ImageTypeCard(
    itemId: UUID,
    type: ImageType,
    images: List<ImageInfo>,
    baseUrl: String,
    busy: Boolean,
    onUpload: (ImageType, Int?) -> Unit,
    onDelete: (ImageType, Int?) -> Unit,
    onMakePrimary: (ImageType, Int) -> Unit,
) {
    Card(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacings.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = imageTypeLabel(type),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            when {
                images.isEmpty() ->
                    EmptyImageRow(
                        busy = busy,
                        onAdd = { onUpload(type, null) },
                    )
                images.size == 1 -> {
                    val image = images.first()
                    SingleImageRow(
                        itemId = itemId,
                        type = type,
                        image = image,
                        baseUrl = baseUrl,
                        busy = busy,
                        onReplace = { onUpload(type, image.imageIndex) },
                        onDelete = { onDelete(type, image.imageIndex) },
                    )
                }
                else ->
                    MultipleImageRow(
                        itemId = itemId,
                        type = type,
                        images = images,
                        baseUrl = baseUrl,
                        busy = busy,
                        onAdd = { onUpload(type, images.size) },
                        onDelete = { image -> onDelete(type, image.imageIndex) },
                        onMakePrimary = { image ->
                            image.imageIndex?.let { onMakePrimary(type, it) }
                        },
                    )
            }
        }
    }
}

@Composable
private fun EmptyImageRow(busy: Boolean, onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        Box(
            modifier =
                Modifier.size(ThumbnailSize)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(CoreR.string.no_image),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAdd, enabled = !busy) { Text(stringResource(CoreR.string.add)) }
    }
}

@Composable
private fun SingleImageRow(
    itemId: UUID,
    type: ImageType,
    image: ImageInfo,
    baseUrl: String,
    busy: Boolean,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        ImageThumbnail(itemId = itemId, type = type, image = image, baseUrl = baseUrl)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            Text(
                text = imageMetadata(image),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                TextButton(onClick = onReplace, enabled = !busy) {
                    Text(stringResource(CoreR.string.replace))
                }
                TextButton(onClick = onDelete, enabled = !busy) {
                    Text(stringResource(CoreR.string.delete))
                }
            }
        }
    }
}

@Composable
private fun MultipleImageRow(
    itemId: UUID,
    type: ImageType,
    images: List<ImageInfo>,
    baseUrl: String,
    busy: Boolean,
    onAdd: () -> Unit,
    onDelete: (ImageInfo) -> Unit,
    onMakePrimary: (ImageInfo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            items(items = images, key = { image -> image.imageIndex ?: image.hashCode() }) { image
                ->
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    ImageThumbnail(
                        itemId = itemId,
                        type = type,
                        image = image,
                        baseUrl = baseUrl,
                        modifier = Modifier.clickable(enabled = !busy) { menuOpen = true },
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(CoreR.string.make_primary)) },
                            enabled = image.imageIndex != 0,
                            onClick = {
                                menuOpen = false
                                onMakePrimary(image)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(CoreR.string.delete)) },
                            onClick = {
                                menuOpen = false
                                onDelete(image)
                            },
                        )
                    }
                }
            }
        }
        Row {
            TextButton(onClick = onAdd, enabled = !busy) { Text(stringResource(CoreR.string.add)) }
        }
    }
}

@Composable
private fun ImageThumbnail(
    itemId: UUID,
    type: ImageType,
    image: ImageInfo,
    baseUrl: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = buildItemImageUrl(baseUrl, itemId, type, image.imageIndex, image.imageTag),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .size(ThumbnailSize)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
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
