package dev.jdtech.jellyfin.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import androidx.media3.ui.DefaultTimeBar
import kotlin.math.PI
import kotlin.math.sin

/**
 * A [DefaultTimeBar] that optionally draws a Material 3 style wavy progress line. Falls back to the
 * standard rendering when [wavyEnabled] is false or the duration is unknown.
 */
class WavyTimeBar
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    DefaultTimeBar(context, attrs, defStyleAttr) {

    var wavyEnabled: Boolean = false

    private var currentPosition: Long = 0L
    private var currentDuration: Long = 0L
    private var currentBuffered: Long = 0L

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val path = Path()

    override fun setPosition(position: Long) {
        currentPosition = position
        super.setPosition(position)
    }

    override fun setDuration(duration: Long) {
        currentDuration = duration
        super.setDuration(duration)
    }

    override fun setBufferedPosition(bufferedPosition: Long) {
        currentBuffered = bufferedPosition
        super.setBufferedPosition(bufferedPosition)
    }

    override fun onDraw(canvas: Canvas) {
        if (!wavyEnabled || currentDuration <= 0L) {
            super.onDraw(canvas)
            return
        }

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) {
            return
        }

        val centerY = height / 2f
        val color = resolveColor(android.R.attr.colorPrimary, Color.WHITE)
        val barThickness = height * 0.22f

        val playedFraction = (currentPosition.toFloat() / currentDuration).coerceIn(0f, 1f)
        val bufferedFraction = (currentBuffered.toFloat() / currentDuration).coerceIn(0f, 1f)
        val playedWidth = width * playedFraction
        val bufferedWidth = width * bufferedFraction

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = barThickness

        // Unplayed track
        paint.color = withAlpha(color, 0.3f)
        canvas.drawLine(0f, centerY, width, centerY, paint)

        // Buffered track
        paint.color = withAlpha(color, 0.5f)
        canvas.drawLine(0f, centerY, bufferedWidth, centerY, paint)

        // Wavy played portion
        paint.color = color
        val amplitude = barThickness * 0.8f
        val wavelength = height * 0.9f
        path.reset()
        path.moveTo(0f, centerY)
        var x = 0f
        while (x <= playedWidth) {
            val y = centerY + amplitude * sin(2.0 * PI * x / wavelength).toFloat()
            path.lineTo(x, y)
            x += 2f
        }
        path.lineTo(playedWidth, centerY)
        canvas.drawPath(path, paint)

        // Scrubber knob
        paint.style = Paint.Style.FILL
        canvas.drawCircle(playedWidth, centerY, barThickness * 0.9f, paint)
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255f).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) value.data else fallback
    }
}
