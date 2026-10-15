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

/** An identify button for the detail screens, shown only to admins. */
@Composable
fun IdentifyAction(
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
                painter = painterResource(CoreR.drawable.ic_database_search),
                contentDescription = stringResource(CoreR.string.identify),
            )
        }
    }
}
