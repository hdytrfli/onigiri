package dev.hdytrfli.onigiri.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import dev.hdytrfli.onigiri.ui.components.PreciseSeekController
import kotlin.math.abs

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
    private var chapterPositions: LongArray = LongArray(0)

    /** When enabled, holding/dragging scrubs relatively; a quick tap still jumps. */
    private var preciseSeekingEnabled = false
    private var onPreciseSeek: ((Long) -> Unit)? = null
    private var seekController: PreciseSeekController? = null
    private var lastPreciseSeekAt = 0L

    fun setPreciseSeeking(enabled: Boolean, onSeek: (Long) -> Unit) {
        preciseSeekingEnabled = enabled
        onPreciseSeek = onSeek
    }

    fun setChapterMarkers(positions: LongArray) {
        if (chapterPositions.contentEquals(positions)) return
        chapterPositions = positions
        invalidate()
    }

    // While the user drags the scrubber, draw from the finger position instead of the playback
    // position (which only updates after the interaction ends).
    private var scrubbing: Boolean = false
    private var scrubMoved: Boolean = false
    private var scrubPosition: Long = 0L
    // After the finger is lifted, keep drawing the scrub position until the player has seeked
    // there, otherwise the periodic position updates snap the bar back to the old position.
    private var awaitingSeek: Boolean = false
    private var animatedPosition: Long = 0L
    private var seekAnimator: ValueAnimator? = null

    private val playedColor: Int
    private val bufferedColor: Int
    private val unplayedColor: Int
    private val scrubberColor: Int
    private val barHeightPx: Float

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

    private val density = resources.displayMetrics.density

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
                    seekAnimator?.cancel()
                    scrubbing = false
                    scrubMoved = false
                    awaitingSeek = false
                    scrubPosition = position
                    invalidate()
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    scrubMoved = true
                    scrubbing = true
                    scrubPosition = position
                    invalidate()
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    scrubbing = false
                    if (canceled) {
                        awaitingSeek = false
                    } else if (scrubMoved) {
                        // Hold the scrub position until the player reports a position near it.
                        scrubPosition = position
                        awaitingSeek = true
                    } else {
                        // A tap jumps to the tapped timestamp: animate the bar to it instead of
                        // snapping. The player seek itself still starts immediately.
                        animateSeekTo(position)
                    }
                    invalidate()
                }
            }
        )
    }

    private fun animateSeekTo(target: Long) {
        seekAnimator?.cancel()
        scrubPosition = target
        awaitingSeek = true
        animatedPosition = currentPosition
        seekAnimator =
            ValueAnimator.ofFloat(currentPosition.toFloat(), target.toFloat()).apply {
                duration = 180L
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    animatedPosition = (it.animatedValue as Float).toLong()
                    invalidate()
                }
                start()
            }
    }

    override fun setPosition(position: Long) {
        currentPosition = position
        if (seekAnimator?.isRunning != true) {
            animatedPosition = position
            if (awaitingSeek && kotlin.math.abs(position - scrubPosition) <= SEEK_TOLERANCE_MS) {
                awaitingSeek = false
            }
        }
        super.setPosition(position)
    }

    override fun onDetachedFromWindow() {
        seekAnimator?.cancel()
        seekAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!preciseSeekingEnabled || currentDuration <= 0L) {
            return super.onTouchEvent(event)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val controller =
                    PreciseSeekController(
                        touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
                        holdToScrubMs = HOLD_TO_SCRUB_MS,
                    )
                seekController = controller
                controller.onDown(
                    x = event.x,
                    width = width.toFloat(),
                    startFraction = currentPosition.toFloat() / currentDuration,
                    timeMs = event.eventTime,
                )
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                seekController?.onMove(event.x, event.eventTime)?.let { fraction ->
                    updatePrecisePosition(fraction)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                when (val result = seekController?.onUp(event.x, event.eventTime)) {
                    is PreciseSeekController.Result.Tap -> {
                        val target = (result.fraction * currentDuration).toLong()
                        seekToTarget(target)
                        animateSeekTo(target)
                    }
                    is PreciseSeekController.Result.Scrub -> {
                        val target = (result.fraction * currentDuration).toLong()
                        seekToTarget(target)
                        scrubbing = false
                        scrubPosition = target
                        awaitingSeek = true
                        invalidate()
                    }
                    null -> Unit
                }
                seekController = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                seekController?.onCancel()
                seekController = null
                scrubbing = false
                awaitingSeek = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updatePrecisePosition(fraction: Float) {
        val target = (fraction * currentDuration).toLong().coerceIn(0L, currentDuration)
        scrubbing = true
        scrubPosition = target
        invalidate()
        val now = SystemClock.elapsedRealtime()
        if (now - lastPreciseSeekAt >= PRECISE_SEEK_INTERVAL_MS) {
            lastPreciseSeekAt = now
            seekToTarget(target)
        }
    }

    private fun seekToTarget(position: Long) {
        onPreciseSeek?.invoke(position)
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

        val position =
            when {
                seekAnimator?.isRunning == true -> animatedPosition
                scrubbing || awaitingSeek -> scrubPosition
                else -> currentPosition
            }
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
            // Rounded on the left, flat on the right (where it meets the handle).
            paint.color = playedColor
            paint.style = Paint.Style.FILL
            val radius = thickness / 2f
            // The unplayed/buffered lines use round caps, so they extend half a thickness past
            // their start; start the played rect half a thickness earlier to line up.
            val rect = RectF(trackStart - radius, centerY - radius, playedX, centerY + radius)
            val radii = floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
            val path = Path()
            path.addRoundRect(rect, radii, Path.Direction.CW)
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.STROKE
        }

        paint.style = Paint.Style.FILL
        paint.color = scrubberColor
        // Chapter ticks sit above the track but below the handle.
        if (currentDuration > 0L && chapterPositions.isNotEmpty()) {
            paint.color =
                Color.argb(
                    220,
                    Color.red(scrubberColor),
                    Color.green(scrubberColor),
                    Color.blue(scrubberColor),
                )
            val markerRadius = 2f * density
            chapterPositions.forEach { chapterPosition ->
                val chapterX =
                    trackStart +
                        trackWidth * (chapterPosition.toFloat() / currentDuration).coerceIn(0f, 1f)
                if (chapterX > trackStart + markerRadius && chapterX < trackEnd - markerRadius) {
                    canvas.drawCircle(chapterX, centerY, markerRadius, paint)
                }
            }
        }

        paint.color = scrubberColor
        // An explicit pill-shaped rect: a round-capped line gets its caps clipped by the View.
        val handleHalfWidth = HANDLE_WIDTH_DP / 2f * density
        val handleHalfHeight = HANDLE_HEIGHT_DP / 2f * density
        val handle =
            RectF(
                playedX - handleHalfWidth,
                centerY - handleHalfHeight,
                playedX + handleHalfWidth,
                centerY + handleHalfHeight,
            )
        canvas.drawRoundRect(handle, handleHalfWidth, handleHalfWidth, paint)
    }
}

/** How close the player's position must be to the scrub position before we trust it again. */
private const val SEEK_TOLERANCE_MS = 1_000L

/** Hold this long before the seek bar switches from tap-to-jump to relative scrubbing. */
private const val HOLD_TO_SCRUB_MS = 180L

/** Minimum interval between player seeks while dragging precisely, to avoid seek spam. */
private const val PRECISE_SEEK_INTERVAL_MS = 50L

/** Handle size in dp: a rounded vertical line, matching the Compose slider thumb. */
private const val HANDLE_WIDTH_DP = 4f
private const val HANDLE_HEIGHT_DP = 16f
