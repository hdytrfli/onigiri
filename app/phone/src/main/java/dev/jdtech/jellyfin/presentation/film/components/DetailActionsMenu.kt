package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyViewModel
import dev.jdtech.jellyfin.film.presentation.mediadetail.SubtitleSearchViewModel
import dev.jdtech.jellyfin.presentation.components.TooltipFilledTonalIconButton
import java.util.UUID

/** Width of the detail actions menu, sized for the longest label. */
private val DetailMenuWidth = 260.dp

/** Corner radius of the detail actions menu, matching the player options sheet. */
private val DetailMenuShape = RoundedCornerShape(28.dp)

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
    identifyViewModel: IdentifyViewModel = hiltViewModel(),
    subtitleViewModel: SubtitleSearchViewModel = hiltViewModel(),
) {
    val identifyState by identifyViewModel.state.collectAsStateWithLifecycle()
    val subtitleState by subtitleViewModel.state.collectAsStateWithLifecycle()

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

    if (!showSubtitleSearch && !showIdentify && !showEditImages) return

    Box(modifier = modifier) {
        TooltipFilledTonalIconButton(
            label = stringResource(CoreR.string.more_options),
            onClick = { menuOpen = true },
        ) {
            if (identifyState.isApplying) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_bolt),
                    contentDescription = stringResource(CoreR.string.more_options),
                )
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.width(DetailMenuWidth),
            shape = DetailMenuShape,
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
        }
    }
}
