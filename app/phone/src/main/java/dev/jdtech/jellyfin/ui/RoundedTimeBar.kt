package dev.jdtech.jellyfin.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar

/**
 * A [DefaultTimeBar] that draws the progress bar with fully rounded ends (both ends), using the
 * colors configured via the standard `app:played_color` / `app:buffered_color` /
 * `app:unplayed_color` / `app:scrubber_color` attributes.
 */
class RoundedTimeBar
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    DefaultTimeBar(context, attrs, defStyleAttr) {

    private var currentPosition: Long = 0L
    private var currentDuration: Long = 0L
    private var currentBuffered: Long = 0L

    // While the user drags the scrubber, draw from the finger position instead of the playback
    // position (which only updates after the interaction ends).
    private var scrubbing: Boolean = false
    private var scrubPosition: Long = 0L
    // After the finger is lifted, keep drawing the scrub position until the player has seeked
    // there, otherwise the periodic position updates snap the bar back to the old position.
    private var awaitingSeek: Boolean = false

    private val playedColor: Int
    private val bufferedColor: Int
    private val unplayedColor: Int
    private val scrubberColor: Int
    private val barHeightPx: Float

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

    init {
        val styleable = androidx.media3.ui.R.styleable.DefaultTimeBar
        val ta = context.obtainStyledAttributes(attrs, styleable, defStyleAttr, 0)
        playedColor =
            ta.getColor(androidx.media3.ui.R.styleable.DefaultTimeBar_played_color, Color.WHITE)
        bufferedColor =
            ta.getColor(
                androidx.media3.ui.R.styleable.DefaultTimeBar_buffered_color,
                0x66FFFFFF,
            )
        unplayedColor =
            ta.getColor(
                androidx.media3.ui.R.styleable.DefaultTimeBar_unplayed_color,
                0x33FFFFFF,
            )
        scrubberColor =
            ta.getColor(androidx.media3.ui.R.styleable.DefaultTimeBar_scrubber_color, Color.WHITE)
        barHeightPx = ta.getDimension(androidx.media3.ui.R.styleable.DefaultTimeBar_bar_height, 0f)
        ta.recycle()

        addListener(
            object : TimeBar.OnScrubListener {
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    scrubbing = true
                    awaitingSeek = false
                    scrubPosition = position
                    invalidate()
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    scrubPosition = position
                    invalidate()
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    scrubbing = false
                    if (canceled) {
                        awaitingSeek = false
                    } else {
                        // Hold the scrub position until the player reports a position near it.
                        scrubPosition = position
                        awaitingSeek = true
                    }
                    invalidate()
                }
            }
        )
    }

    override fun setPosition(position: Long) {
        currentPosition = position
        if (awaitingSeek && kotlin.math.abs(position - scrubPosition) <= SEEK_TOLERANCE_MS) {
            awaitingSeek = false
        }
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
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) {
            return
        }

        val centerY = height / 2f
        val thickness = if (barHeightPx > 0f) barHeightPx else height * 0.25f
        val inset = thickness / 2f
        val trackStart = inset
        val trackEnd = width - inset
        val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)

        val position = if (scrubbing || awaitingSeek) scrubPosition else currentPosition
        // Until the duration is known the bar is just the rounded unplayed track.
        val playedFraction =
            if (currentDuration > 0L) {
                (position.toFloat() / currentDuration).coerceIn(0f, 1f)
            } else {
                0f
            }
        val bufferedFraction =
            if (currentDuration > 0L) {
                (currentBuffered.toFloat() / currentDuration).coerceIn(0f, 1f)
            } else {
                0f
            }
        val playedX = trackStart + trackWidth * playedFraction
        val bufferedX = trackStart + trackWidth * bufferedFraction

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = thickness

        paint.color = unplayedColor
        canvas.drawLine(trackStart, centerY, trackEnd, centerY, paint)

        if (bufferedFraction > 0f) {
            paint.color = bufferedColor
            canvas.drawLine(trackStart, centerY, bufferedX, centerY, paint)
        }

        if (playedFraction > 0f) {
            paint.color = playedColor
            canvas.drawLine(trackStart, centerY, playedX, centerY, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = scrubberColor
        canvas.drawCircle(playedX, centerY, thickness * 0.9f, paint)
    }
}

/** How close the player's position must be to the scrub position before we trust it again. */
private const val SEEK_TOLERANCE_MS = 1_000L
