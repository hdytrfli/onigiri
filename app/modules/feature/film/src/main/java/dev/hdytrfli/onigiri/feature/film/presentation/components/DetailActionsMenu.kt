package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.IdentifyViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.SubtitleSearchViewModel
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppDropdownMenu
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.MenuActionItem
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import java.util.UUID

/** The item-kind dependent actions a detail page can offer in its overflow menu. */
enum class DetailAction {
    SUBTITLE_SEARCH,
    IDENTIFY,
    EDIT_IMAGES,
}

/**
 * The detail pages' overflow menu: a single button listing the actions relevant to the item's kind
 * ([actions]), each shown only while it applies. Subtitle search and identify/edit images are
 * admin-only, gated on the policy loaded by the subtitle/identify view models.
 */
@Composable
fun DetailActionsMenu(
    itemId: UUID,
    actions: Set<DetailAction>,
    modifier: Modifier = Modifier,
    onSubtitleSearch: () -> Unit = {},
    onIdentify: () -> Unit = {},
    onIdentifyApplied: () -> Unit = {},
    onEditImages: () -> Unit = {},
    onDeleted: () -> Unit = {},
    identifyViewModel: IdentifyViewModel = hiltViewModel(),
    subtitleViewModel: SubtitleSearchViewModel = hiltViewModel(),
) {
    val identifyState by identifyViewModel.state.collectAsStateWithLifecycle()
    val subtitleState by subtitleViewModel.state.collectAsStateWithLifecycle()
    val deleteController = rememberMediaDeleteController()

    var menuOpen by remember { mutableStateOf(false) }
    var wasApplying by remember { mutableStateOf(false) }

    LaunchedEffect(itemId, actions) {
        if (DetailAction.IDENTIFY in actions || DetailAction.EDIT_IMAGES in actions) {
            identifyViewModel.loadAdmin()
        }
        if (DetailAction.SUBTITLE_SEARCH in actions) {
            subtitleViewModel.loadAdmin()
        }
    }

    // Identify runs on its own results screen; refresh this page once applying finishes.
    LaunchedEffect(identifyState.isApplying) {
        if (wasApplying && !identifyState.isApplying) onIdentifyApplied()
        wasApplying = identifyState.isApplying
    }

    val showSubtitleSearch =
        DetailAction.SUBTITLE_SEARCH in actions && subtitleState.canManageSubtitles
    val showIdentify = DetailAction.IDENTIFY in actions && identifyState.canManage
    val showEditImages = DetailAction.EDIT_IMAGES in actions && identifyState.canManage
    val showDelete = deleteController.canDelete

    // These actions all need the server, so offline mode hides the trigger entirely.
    if (LocalOfflineMode.current) return
    if (!showSubtitleSearch && !showIdentify && !showEditImages && !showDelete) return

    Box(modifier = modifier) {
        AppIconButton(
            onClick = { menuOpen = true },
            variant = AppButtonVariant.Secondary,
            content = {
                if (identifyState.isApplying) {
                    AppLoadingIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_bolt),
                        contentDescription = stringResource(CoreR.string.more_options),
                    )
                }
            },
        )
        AppDropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            if (showSubtitleSearch) {
                MenuActionItem(
                    icon = CoreR.drawable.ic_case_sensitive,
                    label = stringResource(CoreR.string.search_subtitles),
                    onClick = {
                        menuOpen = false
                        onSubtitleSearch()
                    },
                )
            }
            if (showIdentify) {
                MenuActionItem(
                    icon = CoreR.drawable.ic_text_search,
                    label = stringResource(CoreR.string.identify),
                    onClick = {
                        menuOpen = false
                        onIdentify()
                    },
                )
            }
            if (showEditImages) {
                MenuActionItem(
                    icon = CoreR.drawable.ic_gallery_thumbnails,
                    label = stringResource(CoreR.string.edit_images),
                    onClick = {
                        menuOpen = false
                        onEditImages()
                    },
                )
            }
            if (showDelete) {
                MenuActionItem(
                    icon = CoreR.drawable.ic_lucide_trash,
                    label = stringResource(FilmR.string.delete_media_action),
                    onClick = {
                        menuOpen = false
                        deleteController.request(itemId, onDeleted = onDeleted)
                    },
                )
            }
        }
    }
}
