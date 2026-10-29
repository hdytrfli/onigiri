package dev.jdtech.jellyfin.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton

/** What the reader activity is showing while the publication opens. */
data class ReaderUiState(
    val title: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val progress: String = "",
)

private val OverlayScrim =
    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))
private val BottomScrim =
    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))

/**
 * The reader's chrome, drawn over the navigator: a top bar with the title and the settings, a
 * bottom bar with the reading progress, and the settings sheet itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderOverlay(
    state: ReaderUiState,
    settings: ReaderSettings,
    showSettings: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onSettingsChange: (ReaderSettings) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading ->
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    CircularProgressIndicator()
                }

            state.error != null ->
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(text = state.error, style = MaterialTheme.typography.bodyLarge)
                    AppButton(onClick = onBack) { Text("Close") }
                }

            else -> {
                Row(
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(OverlayScrim)
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconButton(onClick = onBack, variant = AppButtonVariant.Ghost) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = "Back",
                        )
                    }
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    AppIconButton(onClick = onOpenSettings, variant = AppButtonVariant.Ghost) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_tune),
                            contentDescription = "Reader settings",
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(BottomScrim)
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.progress,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    AppButton(onClick = onOpenSettings, variant = AppButtonVariant.Ghost) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_tune),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Aa")
                    }
                }
            }
        }

        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = onDismissSettings,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    Text(
                        text = "Reader settings",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                    )
                    ReaderSettingsContent(
                        settings = settings,
                        onSettingsChange = onSettingsChange,
                    )
                }
            }
        }
    }
}
