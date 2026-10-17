package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import dev.jdtech.jellyfin.core.presentation.theme.Dimens
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun BaseDialog(
    title: String,
    onDismiss: () -> Unit,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    padding: Dp = MaterialTheme.spacings.default,
    maxWidth: Dp? = null,
    content: @Composable ColumnScope.(contentPadding: PaddingValues) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier =
                Modifier.then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
                    .fillMaxWidth()
                    .heightIn(max = Dimens.dialogMaxHeight),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(modifier = Modifier.padding(top = padding)) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = padding).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = titleStyle,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                content(PaddingValues(horizontal = padding))
            }
        }
    }
}

@Composable
fun BaseDialog(
    title: String,
    onDismiss: () -> Unit,
    negativeButton: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    content: @Composable ColumnScope.(contentPadding: PaddingValues) -> Unit,
) {
    BaseDialog(title = title, onDismiss = onDismiss) { contentPadding ->
        content(contentPadding)
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacings.default).fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            negativeButton()
            positiveButton()
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
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
            negativeButton = { TextButton(onClick = {}) { Text(text = "Negative") } },
            positiveButton = { TextButton(onClick = {}) { Text(text = "Positive") } },
            onDismiss = {},
        ) {
            Box(modifier = Modifier.fillMaxSize().weight(1f, fill = false).background(Color.Red))
        }
    }
}
