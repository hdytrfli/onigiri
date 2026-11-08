package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.R
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class MediaDeleteState(
    val canDelete: Boolean = false,
    val isDeleting: Boolean = false,
    val error: Throwable? = null,
)

@HiltViewModel
class MediaDeleteViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(MediaDeleteState())
    val state = _state.asStateFlow()

    private var permissionLoaded = false

    fun loadPermission() {
        if (permissionLoaded) return
        permissionLoaded = true
        viewModelScope.launch {
            val canDelete = runCatching {
                repository.getUserPolicy()?.let {
                    it.isAdministrator || it.enableContentDeletion
                } == true
            }
                .getOrElse { false }
            _state.update { it.copy(canDelete = canDelete) }
        }
    }

    fun delete(itemId: UUID, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, error = null) }
            runCatching { repository.deleteItem(itemId) }
                .onSuccess {
                    _state.update { it.copy(isDeleting = false) }
                    onDeleted()
                }
                .onFailure { error ->
                    Timber.w(error, "Unable to delete media item $itemId")
                    _state.update { it.copy(isDeleting = false, error = error) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

private data class DeleteRequest(
    val itemId: UUID,
    val title: String?,
    val onDeleted: () -> Unit,
)

class MediaDeleteController
internal constructor(
    val canDelete: Boolean,
    private val requestDelete: (UUID, String?, () -> Unit) -> Unit,
) {
    fun request(itemId: UUID, title: String? = null, onDeleted: () -> Unit) {
        requestDelete(itemId, title, onDeleted)
    }
}

@Composable
fun rememberMediaDeleteController(): MediaDeleteController {
    val viewModel: MediaDeleteViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<DeleteRequest?>(null) }

    LaunchedEffect(Unit) { viewModel.loadPermission() }

    val controller =
        remember(state.canDelete) {
            MediaDeleteController(state.canDelete) { itemId, title, onDeleted ->
                pending = DeleteRequest(itemId, title, onDeleted)
            }
        }

    pending?.let { request ->
        BaseDialog(
            title = stringResource(R.string.delete_media_title),
            description =
                request.title?.let { stringResource(R.string.delete_media_message, it) }
                    ?: stringResource(R.string.delete_media_message_generic),
            onDismiss = { pending = null },
            negativeButton = {
                AppButton(
                    onClick = { pending = null },
                    variant = AppButtonVariant.Secondary,
                    enabled = !state.isDeleting,
                ) {
                    Text(stringResource(dev.hdytrfli.onigiri.core.R.string.cancel))
                }
            },
            positiveButton = {
                AppButton(
                    onClick = {
                        viewModel.delete(request.itemId) {
                            pending = null
                            request.onDeleted()
                        }
                    },
                    variant = AppButtonVariant.Destructive,
                    enabled = !state.isDeleting,
                ) {
                    Text(stringResource(dev.hdytrfli.onigiri.core.R.string.delete))
                }
            },
        ) { contentPadding ->
            Text(
                text =
                    state.error?.localizedMessage ?: stringResource(R.string.delete_media_warning),
                modifier = androidx.compose.ui.Modifier.padding(contentPadding),
                color =
                    if (state.error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }

    return controller
}
