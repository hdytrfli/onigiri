package dev.hdytrfli.onigiri.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

/** The semantic colour and icon treatment of an [AppSnackbar]. */
enum class AppSnackbarTone {
    Neutral,
    Error,
    Offline,
}

private data class AppSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val withDismissAction: Boolean,
    override val duration: SnackbarDuration,
    val tone: AppSnackbarTone,
) : SnackbarVisuals

/**
 * Shows a snackbar using the app's snackbar style. [onAction] runs only when the user presses the
 * action button and the snackbar reports [SnackbarResult.ActionPerformed].
 */
suspend fun SnackbarHostState.showAppSnackbar(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = SnackbarDuration.Short,
    tone: AppSnackbarTone = AppSnackbarTone.Neutral,
    onAction: () -> Unit = {},
): SnackbarResult {
    val result =
        showSnackbar(
            AppSnackbarVisuals(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
                duration = duration,
                tone = tone,
            )
        )
    if (result == SnackbarResult.ActionPerformed) onAction()
    return result
}

@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data -> AppSnackbar(data) }
}

@Composable
fun AppSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val visuals = data.visuals as? AppSnackbarVisuals
    AppSnackbarContent(
        message = data.visuals.message,
        actionLabel = data.visuals.actionLabel,
        withDismissAction = data.visuals.withDismissAction,
        tone = visuals?.tone ?: AppSnackbarTone.Neutral,
        onAction = data::performAction,
        onDismiss = data::dismiss,
        modifier = modifier,
    )
}

@Composable
private fun AppSnackbarContent(
    message: String,
    actionLabel: String?,
    withDismissAction: Boolean,
    tone: AppSnackbarTone,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = tone.colors()
    val icon = tone.icon()

    Surface(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(min = 52.dp),
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.content,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null) {
                CompositionLocalProvider(LocalContentColor provides colors.action) {
                    AppButton(
                        onClick = onAction,
                        size = AppButtonSize.Decorator,
                        variant = AppButtonVariant.Ghost,
                    ) {
                        Text(text = actionLabel)
                    }
                }
            }
            if (withDismissAction) {
                CompositionLocalProvider(LocalContentColor provides colors.action) {
                    AppIconButton(
                        onClick = onDismiss,
                        size = AppButtonSize.Decorator,
                        variant = AppButtonVariant.Ghost,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_x),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

private data class AppSnackbarColors(
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val action: androidx.compose.ui.graphics.Color,
)

@Composable
private fun AppSnackbarTone.colors(): AppSnackbarColors =
    when (this) {
        AppSnackbarTone.Neutral ->
            AppSnackbarColors(
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = MaterialTheme.colorScheme.onSurface,
                action = MaterialTheme.colorScheme.primary,
            )
        AppSnackbarTone.Error ->
            AppSnackbarColors(
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
                action = MaterialTheme.colorScheme.onErrorContainer,
            )
        AppSnackbarTone.Offline ->
            AppSnackbarColors(
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                action = MaterialTheme.colorScheme.onSecondaryContainer,
            )
    }

@DrawableRes
private fun AppSnackbarTone.icon(): Int? =
    when (this) {
        AppSnackbarTone.Neutral -> null
        AppSnackbarTone.Error -> CoreR.drawable.ic_alert_circle
        AppSnackbarTone.Offline -> CoreR.drawable.ic_cloud_off
    }

@Preview
@Composable
private fun AppSnackbarPreview() {
    FindroidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppSnackbarContent(
                message = "Saved to your favorites",
                actionLabel = "Undo",
                withDismissAction = false,
                tone = AppSnackbarTone.Neutral,
                onAction = {},
                onDismiss = {},
            )
            AppSnackbarContent(
                message = "Couldn't refresh. Showing cached data.",
                actionLabel = "Retry",
                withDismissAction = true,
                tone = AppSnackbarTone.Offline,
                onAction = {},
                onDismiss = {},
            )
            AppSnackbarContent(
                message = "Something went wrong",
                actionLabel = null,
                withDismissAction = false,
                tone = AppSnackbarTone.Error,
                onAction = {},
                onDismiss = {},
            )
        }
    }
}
