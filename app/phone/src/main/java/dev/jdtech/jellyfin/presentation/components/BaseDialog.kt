package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jdtech.jellyfin.core.presentation.theme.Dimens
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * The one dialog in the app.
 *
 * The card holds the title, an optional one-line-of-context description and the content. Actions
 * sit outside the card, anchored to its bottom-right, so the dialog body stays content-only.
 *
 * @param description a short explanation; it is suppressed when blank or equal to [title].
 * @param actions the action buttons (use [AppButton]); omit for dialogs dismissed another way.
 */
@Composable
fun BaseDialog(
    title: String,
    onDismiss: () -> Unit,
    description: String? = null,
    content: @Composable ColumnScope.(contentPadding: PaddingValues) -> Unit,
) {
    BaseDialogImpl(
        title = title,
        onDismiss = onDismiss,
        description = description,
        actions = null,
        content = content,
    )
}

@Composable
fun BaseDialog(
    title: String,
    onDismiss: () -> Unit,
    description: String? = null,
    negativeButton: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    content: @Composable ColumnScope.(contentPadding: PaddingValues) -> Unit,
) {
    BaseDialogImpl(
        title = title,
        onDismiss = onDismiss,
        description = description,
        actions = {
            negativeButton()
            positiveButton()
        },
        content = content,
    )
}

@Composable
private fun BaseDialogImpl(
    title: String,
    onDismiss: () -> Unit,
    description: String?,
    actions: (@Composable RowScope.() -> Unit)?,
    content: @Composable ColumnScope.(contentPadding: PaddingValues) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.widthIn(max = Dimens.dialogMaxWidth).fillMaxWidth()) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = Dimens.dialogCompactMaxHeight),
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
            ) {
                Column {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                        if (!description.isNullOrBlank() && description != title) {
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    content(PaddingValues(horizontal = MaterialTheme.spacings.medium))
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                }
            }

            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Preview
@Composable
private fun BaseDialogPreview() {
    FindroidTheme {
        BaseDialog(title = "Dialog Title", onDismiss = {}) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Red))
        }
    }
}

@Preview
@Composable
private fun BaseDialogButtonsPreview() {
    FindroidTheme {
        BaseDialog(
            title = "Dialog Title",
            description = "A short explanation that stays on one or two lines.",
            onDismiss = {},
            negativeButton = {
                AppButton(onClick = {}, variant = AppButtonVariant.Ghost) { Text("Cancel") }
            },
            positiveButton = { AppButton(onClick = {}) { Text("Confirm") } },
        ) {
            Box(modifier = Modifier.fillMaxSize().weight(1f, fill = false).background(Color.Red))
        }
    }
}
