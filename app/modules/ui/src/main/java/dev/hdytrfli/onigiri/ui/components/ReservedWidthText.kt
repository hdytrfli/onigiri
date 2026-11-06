package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Text whose width is reserved from [referenceText].
 *
 * Elapsed-time and current-page labels grow digit by digit, which otherwise makes the neighbouring
 * seek bar shrink and jump while scrubbing. Measuring the largest label up front keeps the bar
 * width fixed for the whole interaction.
 */
@Composable
fun ReservedWidthText(
    text: String,
    referenceText: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.End,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val reservedWidth =
        remember(measurer, referenceText, style, density) {
            with(density) { measurer.measure(referenceText, style).size.width.toDp() }
        }
    Text(
        text = text,
        modifier = modifier.widthIn(min = reservedWidth),
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}
