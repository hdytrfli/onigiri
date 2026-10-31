package dev.jdtech.jellyfin.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A single hidden text field rendered as a row of boxes. Keeping one field rather than one field
 * per cell preserves paste, backspace and IME behaviour.
 */
@Composable
fun PinCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int,
    modifier: Modifier = Modifier,
    masked: Boolean = true,
    isError: Boolean = false,
    readOnly: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val gap = 8.dp
    val borderColor =
        if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        }

    LaunchedEffect(readOnly) { if (!readOnly) focusRequester.requestFocus() }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val available = maxWidth - gap * (length - 1)
        val cellWidth = (available / length.toFloat()).coerceAtMost(56.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(length) { index ->
                val character = value.getOrNull(index)
                Box(
                    modifier =
                        Modifier.width(cellWidth)
                            .height(56.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, borderColor, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    if (character != null) {
                        Text(
                            text = if (masked) "•" else character.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(length)) },
            singleLine = true,
            enabled = !readOnly,
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions.Default,
            modifier = Modifier.matchParentSize().focusRequester(focusRequester),
        )
    }
}
