package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.ui.theme.spacings

/**
 * The app's text input: the filled rounded box used by the Identify form, not Material's outlined
 * field. The label sits above the box; [supportingText] doubles as the error message.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            enabled = enabled,
            textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color =
                        if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(
                                1.dp,
                                if (isError) MaterialTheme.colorScheme.error else Color.Transparent,
                                shape,
                            )
                            .padding(
                                horizontal = MaterialTheme.spacings.medium,
                                vertical =
                                    if (leadingContent != null || trailingContent != null) {
                                        14.dp
                                    } else {
                                        12.dp
                                    },
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingContent?.let {
                        it()
                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && hint != null) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyLarge,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                    if (trailingText != null) {
                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                        Text(
                            text = trailingText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    trailingContent?.let {
                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                        it()
                    }
                }
            },
        )

        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}
