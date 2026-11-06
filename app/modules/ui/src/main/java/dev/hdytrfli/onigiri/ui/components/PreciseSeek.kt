package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

/**
 * Shared tap-vs-hold decision for seek bars.
 *
 * A quick tap is a jump. A hold, or any drag, becomes relative precise scrubbing: the fraction
 * changes by the horizontal distance travelled divided by the bar width. The controller is pure
 * Kotlin so the video player's native time bar and the Compose seek bars use the same rules.
 */
class PreciseSeekController(
    private val touchSlopPx: Float,
    private val holdToScrubMs: Long = 180L,
) {
    sealed interface Result {
        data class Tap(val fraction: Float) : Result

        data class Scrub(val fraction: Float) : Result
    }

    private var downX = 0f
    private var barWidth = 1f
    private var downFraction = 0f
    private var downAt = 0L
    private var tapCandidate = false
    private var precisionActive = false
    private var moved = false
    private var currentFraction = 0f

    fun onDown(x: Float, width: Float, startFraction: Float, timeMs: Long) {
        downX = x
        barWidth = width.coerceAtLeast(1f)
        downFraction = startFraction.coerceIn(0f, 1f)
        downAt = timeMs
        tapCandidate = true
        precisionActive = false
        moved = false
        currentFraction = downFraction
    }

    /** Returns the new fraction while precise scrubbing is active, otherwise null. */
    fun onMove(x: Float, timeMs: Long): Float? {
        if (!tapCandidate && !precisionActive) return null
        val dx = x - downX
        if (!precisionActive) {
            val held = timeMs - downAt >= holdToScrubMs
            if (!held && abs(dx) <= touchSlopPx) return null
            precisionActive = true
            tapCandidate = false
        }
        if (abs(dx) > touchSlopPx) moved = true
        currentFraction = (downFraction + dx / barWidth).coerceIn(0f, 1f)
        return currentFraction
    }

    fun onUp(x: Float, timeMs: Long): Result? {
        val held = timeMs - downAt >= holdToScrubMs
        currentFraction = (downFraction + (x - downX) / barWidth).coerceIn(0f, 1f)
        val result =
            when {
                tapCandidate && !precisionActive && !held ->
                    Result.Tap((x / barWidth).coerceIn(0f, 1f))
                precisionActive && moved -> Result.Scrub(currentFraction)
                else -> null
            }
        tapCandidate = false
        precisionActive = false
        moved = false
        return result
    }

    fun onCancel() {
        tapCandidate = false
        precisionActive = false
        moved = false
    }
}

/**
 * Adds tap-to-jump / hold-to-scrub behaviour to a seek bar. [startFraction] is read when the
 * gesture begins; [onScrub] is called continuously while the finger moves.
 */
@Composable
fun Modifier.preciseSeekable(
    enabled: Boolean,
    startFraction: () -> Float,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    onTap: (Float) -> Unit,
): Modifier {
    val latestStartFraction by rememberUpdatedState(startFraction)
    val latestOnScrub by rememberUpdatedState(onScrub)
    val latestOnScrubEnd by rememberUpdatedState(onScrubEnd)
    val latestOnTap by rememberUpdatedState(onTap)

    if (!enabled) return this
    return pointerInput(enabled) {
        val controller = PreciseSeekController(touchSlopPx = viewConfiguration.touchSlop)
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            controller.onDown(
                x = down.position.x,
                width = size.width.toFloat(),
                startFraction = latestStartFraction(),
                timeMs = down.uptimeMillis,
            )
            down.consume()
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUpIgnoreConsumed()) {
                    when (val result = controller.onUp(change.position.x, change.uptimeMillis)) {
                        is PreciseSeekController.Result.Tap -> latestOnTap(result.fraction)
                        is PreciseSeekController.Result.Scrub -> latestOnScrubEnd(result.fraction)
                        null -> Unit
                    }
                    change.consume()
                    break
                }
                if (change.positionChanged()) {
                    controller.onMove(change.position.x, change.uptimeMillis)?.let(latestOnScrub)
                    change.consume()
                }
            }
        }
    }
}
