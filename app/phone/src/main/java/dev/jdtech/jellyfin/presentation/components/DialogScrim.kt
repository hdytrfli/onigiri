package dev.jdtech.jellyfin.presentation.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Tracks how many dialogs are open so the app content can be blurred behind them. */
@Stable
class DialogScrimState {
    private var count by mutableIntStateOf(0)

    val isActive: Boolean
        get() = count > 0

    fun show() {
        count++
    }

    fun hide() {
        count = (count - 1).coerceAtLeast(0)
    }
}

val LocalDialogScrim = compositionLocalOf { DialogScrimState() }
