package dev.hdytrfli.onigiri.feature.film.presentation.components

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
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.components.AppButtonGroup
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.LocalTopBarExternalInset
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

@Composable
fun ItemTopBar(
    hasBackButton: Boolean,
    onBackClick: () -> Unit = {},
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable (RowScope.() -> Unit) = {},
) {
    val safePadding = rememberSafePadding()
    val includeOwnTopInset = !LocalTopBarExternalInset.current

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    start = safePadding.start + MaterialTheme.spacings.gutter,
                    top =
                        if (includeOwnTopInset) {
                            safePadding.top + MaterialTheme.spacings.gutter
                        } else {
                            0.dp
                        },
                    end = safePadding.end + MaterialTheme.spacings.gutter,
                )
    ) {
        AppButtonGroup {
            if (hasBackButton) {
                AppIconButton(
                    onClick = onBackClick,
                    variant = AppButtonVariant.Secondary,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
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
    FindroidTheme { ItemTopBar(hasBackButton = true) }
}
