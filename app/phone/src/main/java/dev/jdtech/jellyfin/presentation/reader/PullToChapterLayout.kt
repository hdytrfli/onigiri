package dev.jdtech.jellyfin.presentation.reader

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import dev.jdtech.jellyfin.R
import kotlin.math.abs
import kotlin.math.max

/**
 * Sits between Readium's navigator and the reader chrome. Readium owns all normal scrolling and
 * formatting; this layout only watches for an outward pull at the top or bottom of a chapter and
 * turns it into a chapter change.
 */
class PullToChapterLayout
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class Direction {
        NEXT,
        PREVIOUS,
    }

    var pullEnabled: Boolean = false

    var canPullNext: (() -> Boolean)? = null

    var canPullPrevious: (() -> Boolean)? = null

    var onPullNext: (() -> Unit)? = null

    var onPullPrevious: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val triggerDistance = 112f * density
    private val maxPull = triggerDistance * 1.35f

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6000000.toInt() }
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 14f * scaledDensity
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val pillRect = RectF()

    private var downX = 0f
    private var downY = 0f
    private var pullDistance = 0f
    private var pulling = false
    private var direction = Direction.NEXT
    private var resetAnimator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // Readium's pagers ask the parent not to intercept their gestures. We still need to see
        // vertical pulls at chapter boundaries, so keep our own intercept pass alive.
        super.requestDisallowInterceptTouchEvent(false)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!pullEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetAnimator?.cancel()
                resetAnimator = null
                downX = event.x
                downY = event.y
                pullDistance = 0f
                pulling = false
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (pulling) return true
                val dy = event.y - downY
                val dx = event.x - downX
                if (abs(dy) < touchSlop || abs(dy) <= abs(dx)) return false

                val canPull =
                    if (dy < 0) {
                        canPullNext?.invoke() ?: false
                    } else {
                        canPullPrevious?.invoke() ?: false
                    }
                if (!canPull) return false

                direction = if (dy < 0) Direction.NEXT else Direction.PREVIOUS
                pulling = true
                pullDistance = abs(dy).coerceIn(0f, maxPull)
                invalidate()
                return true
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!pulling) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val raw =
                    if (direction == Direction.NEXT) {
                        downY - event.y
                    } else {
                        event.y - downY
                    }
                pullDistance = raw.coerceIn(0f, maxPull)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val triggered = pullDistance >= triggerDistance
                val completedDirection = direction
                pulling = false
                if (triggered) {
                    when (completedDirection) {
                        Direction.NEXT -> onPullNext?.invoke()
                        Direction.PREVIOUS -> onPullPrevious?.invoke()
                    }
                }
                animateReset()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pulling = false
                animateReset()
                return true
            }
        }
        return true
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (!pullEnabled || pullDistance <= 0f) return

        val progress = (pullDistance / triggerDistance).coerceIn(0f, 1f)
        val alpha = (progress * 1.6f).coerceIn(0f, 1f)
        val releasing = progress >= 1f
        val label =
            when (direction) {
                Direction.NEXT ->
                    context.getString(
                        if (releasing) R.string.reader_pull_next_release
                        else R.string.reader_pull_next_chapter
                    )
                Direction.PREVIOUS ->
                    context.getString(
                        if (releasing) R.string.reader_pull_previous_release
                        else R.string.reader_pull_previous_chapter
                    )
            }

        val textWidth = textPaint.measureText(label)
        val pillWidth = max(180f * density, textWidth + 48f * density)
        val pillHeight = 44f * density
        val centerX = width / 2f
        val baseY =
            when (direction) {
                Direction.NEXT -> height - 96f * density - pillHeight
                Direction.PREVIOUS -> 96f * density
            }
        val slide = (1f - progress) * 18f * density
        val top =
            when (direction) {
                Direction.NEXT -> baseY + slide
                Direction.PREVIOUS -> baseY - slide
            }

        pillRect.set(
            centerX - pillWidth / 2f,
            top,
            centerX + pillWidth / 2f,
            top + pillHeight,
        )

        val oldPillAlpha = pillPaint.alpha
        val oldTextAlpha = textPaint.alpha
        val oldProgressAlpha = progressPaint.alpha
        pillPaint.alpha = (alpha * oldPillAlpha).toInt()
        textPaint.alpha = (alpha * oldTextAlpha).toInt()
        progressPaint.alpha = (alpha * oldProgressAlpha).toInt()

        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillPaint)

        val textBaseline = pillRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(label, centerX, textBaseline, textPaint)

        val barInset = 14f * density
        val barTop = pillRect.bottom - 8f * density
        val barWidth = (pillRect.width() - barInset * 2f) * progress
        canvas.drawRoundRect(
            pillRect.left + barInset,
            barTop,
            pillRect.left + barInset + barWidth,
            barTop + 2.5f * density,
            2.5f * density,
            2.5f * density,
            progressPaint,
        )

        pillPaint.alpha = oldPillAlpha
        textPaint.alpha = oldTextAlpha
        progressPaint.alpha = oldProgressAlpha
    }

    private fun animateReset() {
        resetAnimator?.cancel()
        if (pullDistance <= 0f) {
            invalidate()
            return
        }
        resetAnimator =
            ValueAnimator.ofFloat(pullDistance, 0f).apply {
                duration = 160L
                addUpdateListener { animator ->
                    pullDistance = animator.animatedValue as Float
                    invalidate()
                }
                start()
            }
    }

    override fun onDetachedFromWindow() {
        resetAnimator?.cancel()
        resetAnimator = null
        super.onDetachedFromWindow()
    }
}
