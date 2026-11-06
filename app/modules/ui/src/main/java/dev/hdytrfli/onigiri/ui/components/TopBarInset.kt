package dev.hdytrfli.onigiri.ui.components

import androidx.compose.runtime.compositionLocalOf

/** Set by TopBarPageShell when the shell itself already owns the top window inset. */
val LocalTopBarExternalInset = compositionLocalOf { false }
