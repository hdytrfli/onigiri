package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
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
import java.util.UUID

/**
 * An identify button for the detail screens, shown only to admins. While a chosen result is being
 * applied it shows a loading indicator, then calls [onApplied] to refresh the details.
 */
@Composable
fun IdentifyAction(
    itemId: UUID,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onApplied: () -> Unit = {},
    viewModel: IdentifyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(itemId) { viewModel.loadAdmin() }

    var wasApplying by remember { mutableStateOf(false) }
    LaunchedEffect(state.isApplying) {
        if (wasApplying && !state.isApplying) onApplied()
        wasApplying = state.isApplying
    }

    if (state.canManage) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier) {
            if (state.isApplying) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_text_search),
                    contentDescription = stringResource(CoreR.string.identify),
                )
            }
        }
    }
}
