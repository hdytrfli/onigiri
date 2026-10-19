package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppButtonGroup
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

@Composable
fun ItemTopBar(
    hasBackButton: Boolean,
    hasHomeButton: Boolean,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable (RowScope.() -> Unit) = {},
) {
    val safePadding = rememberSafePadding()

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    start = safePadding.start + MaterialTheme.spacings.small,
                    top = safePadding.top + MaterialTheme.spacings.small,
                    end = safePadding.end + MaterialTheme.spacings.small,
                )
    ) {
        AppButtonGroup {
            if (hasBackButton) {
                AppIconButton(
                    onClick = onBackClick,
                    variant = AppButtonVariant.Overlay,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    },
                )
            }
            if (hasHomeButton) {
                AppIconButton(
                    onClick = onHomeClick,
                    variant = AppButtonVariant.Overlay,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_home),
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        content()
        Spacer(modifier = Modifier.weight(1f))
        trailingContent?.invoke(this)
    }
}

@Composable
@Preview(showBackground = true)
private fun ItemTopBarPreview() {
    FindroidTheme { ItemTopBar(hasBackButton = true, hasHomeButton = true) }
}
