package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.jdtech.jellyfin.core.presentation.theme.Spacings

@Composable
fun FindroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkScheme, shapes = shapes) {
        CompositionLocalProvider(
            LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.background),
            LocalSpacings provides Spacings,
        ) {
            content()
        }
    }
}
