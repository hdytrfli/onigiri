package dev.hdytrfli.onigiri.presentation.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hdytrfli.onigiri.core.R as CoreR

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
    loading: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val gap = 8.dp
    val cellColor = MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor =
        if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            Color.Transparent
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
                            .background(cellColor)
                            .border(1.dp, borderColor, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading) {
                        val transition =
                            rememberInfiniteTransition(label = "quick-connect-dot-$index")
                        val offsetY by
                            transition.animateFloat(
                                initialValue = 0f,
                                targetValue = 0f,
                                animationSpec =
                                    infiniteRepeatable(
                                        animation =
                                            keyframes {
                                                durationMillis = 1600
                                                0f at 0
                                                -6f at 160 using FastOutSlowInEasing
                                                0f at 320 using FastOutSlowInEasing
                                                0f at 1599
                                            },
                                        initialStartOffset = StartOffset(index * 100),
                                    ),
                                label = "quick-connect-dot-offset-$index",
                            )
                        Box(
                            modifier =
                                Modifier.offset(y = offsetY.dp)
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                        )
                    } else if (character != null) {
                        if (masked) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_asterisk),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Text(
                                text = character.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
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
