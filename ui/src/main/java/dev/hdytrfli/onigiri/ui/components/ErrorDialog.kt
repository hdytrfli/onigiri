package dev.hdytrfli.onigiri.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun ErrorDialog(exception: Throwable, onDismissRequest: () -> Unit) {
    val context = LocalContext.current

    BaseDialog(
        title = exception.message ?: stringResource(CoreR.string.unknown_error),
        onDismiss = onDismissRequest,
        negativeButton = {
            AppButton(
                variant = AppButtonVariant.Ghost,
                onClick = {
                    val sendIntent =
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${exception.message}\n ${exception.stackTraceToString()}",
                            )
                            type = "text/plain"
                        }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                },
            ) {
                Text(stringResource(CoreR.string.share))
            }
        },
        positiveButton = {
            AppButton(onClick = onDismissRequest) { Text(stringResource(CoreR.string.close)) }
        },
    ) { contentPadding ->
        Text(
            text = exception.stackTraceToString(),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
        )
    }
}

@Preview
@Composable
private fun ErrorDialogPreview() {
    FindroidTheme {
        ErrorDialog(exception = Exception("Error loading data"), onDismissRequest = {})
    }
}
