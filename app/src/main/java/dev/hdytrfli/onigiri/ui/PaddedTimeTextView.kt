package dev.hdytrfli.onigiri.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.Locale

/**
 * The media3 controls write the current position directly into a TextView. Their formatter omits
 * leading zeroes and drops the hour field when the current position is below one hour, which makes
 * the label jump around while the total duration stays fixed. Reformat every incoming timestamp to
 * match the total duration's shape.
 */
class PaddedTimeTextView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatTextView(context, attrs, defStyleAttr) {

    var showHours: Boolean = false

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(format(text), type)
    }

    private fun format(text: CharSequence?): CharSequence? {
        val value = text?.toString()?.trim().orEmpty()
        val parts = value.split(':')
        if (parts.size != 2 && parts.size != 3) return text
        if (parts.any { part -> part.isEmpty() || part.any { !it.isDigit() } }) return text

        val seconds = parts.last().toIntOrNull() ?: return text
        val minutes = parts[parts.lastIndex - 1].toIntOrNull() ?: return text
        return if (showHours) {
            val hours = if (parts.size == 3) parts.first().toIntOrNull() ?: 0 else 0
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
