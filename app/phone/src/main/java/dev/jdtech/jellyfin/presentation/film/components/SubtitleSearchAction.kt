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
import dev.jdtech.jellyfin.film.presentation.mediadetail.SubtitleSearchViewModel
import java.util.UUID

/**
 * A subtitle button for the detail screens, shown only to admins. It opens the remote-subtitle
 * screen where the user can pick a language, search, download and delete subtitles.
 */
@Composable
fun SubtitleSearchAction(
    itemId: UUID,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubtitleSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The admin policy is cached upstream, so this only needs a network round-trip the first time.
    LaunchedEffect(itemId) { viewModel.loadAdmin() }

    if (state.canManageSubtitles) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_closed_caption),
                contentDescription = stringResource(CoreR.string.search_subtitles),
            )
        }
    }
}
