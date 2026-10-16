package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyViewModel
import java.util.UUID

/**
 * An "edit images" button for the detail screens, shown only to admins. It opens the image editor
 * where images can be uploaded, replaced, deleted and reordered. Reuses the admin policy loaded by
 * [IdentifyViewModel], which is cached upstream.
 */
@Composable
fun EditImagesAction(
    itemId: UUID,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IdentifyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(itemId) { viewModel.loadAdmin() }

    if (state.canManage) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_gallery_thumbnails),
                contentDescription = stringResource(CoreR.string.edit_images),
            )
        }
    }
}
