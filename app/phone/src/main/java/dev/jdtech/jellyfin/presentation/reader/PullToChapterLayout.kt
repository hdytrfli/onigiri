package dev.jdtech.jellyfin.presentation.reader

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import dev.jdtech.jellyfin.core.R as CoreR
import kotlin.math.abs

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
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val triggerDistance = 112f * density
    private val maxPull = triggerDistance * 1.35f
    private val circleRadius = 24f * density
    private val ringWidth = 3f * density

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD9000000.toInt() }
    private val trackPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = ringWidth
            strokeCap = Paint.Cap.ROUND
        }
    private val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = ringWidth
            strokeCap = Paint.Cap.ROUND
        }
    private val ringRect = RectF()
    private val nextIcon: Drawable? =
        ContextCompat.getDrawable(context, CoreR.drawable.ic_chevron_down)?.mutate()?.apply {
            setTint(0xFFFFFFFF.toInt())
        }
    private val previousIcon: Drawable? =
        ContextCompat.getDrawable(context, CoreR.drawable.ic_chevron_up)?.mutate()?.apply {
            setTint(0xFFFFFFFF.toInt())
        }

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
        val alpha = (0.35f + 0.65f * progress).coerceIn(0f, 1f)
        val centerX = width / 2f
        val baseY =
            when (direction) {
                Direction.NEXT -> height - 96f * density
                Direction.PREVIOUS -> 96f * density
            }
        val slide = (1f - progress) * 12f * density
        val centerY =
            when (direction) {
                Direction.NEXT -> baseY + slide
                Direction.PREVIOUS -> baseY - slide
            }
        val scale = 0.86f + 0.14f * progress

        canvas.save()
        canvas.scale(scale, scale, centerX, centerY)

        fillPaint.alpha = (alpha * 255).toInt()
        trackPaint.alpha = (alpha * 85).toInt()
        ringPaint.alpha = (alpha * 255).toInt()

        val fillRadius = circleRadius - ringWidth
        canvas.drawCircle(centerX, centerY, fillRadius, fillPaint)

        val ringRadius = circleRadius - ringWidth / 2f
        ringRect.set(
            centerX - ringRadius,
            centerY - ringRadius,
            centerX + ringRadius,
            centerY + ringRadius,
        )
        canvas.drawArc(ringRect, -90f, 360f, false, trackPaint)
        canvas.drawArc(ringRect, -90f, 360f * progress, false, ringPaint)

        val icon = if (direction == Direction.NEXT) nextIcon else previousIcon
        if (icon != null) {
            val iconSize = (24f * density).toInt()
            val half = iconSize / 2
            icon.setBounds(
                centerX.toInt() - half,
                centerY.toInt() - half,
                centerX.toInt() + half,
                centerY.toInt() + half,
            )
            icon.alpha = (alpha * 255).toInt()
            icon.draw(canvas)
        }

        canvas.restore()
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
