package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * The in-place search header: the toolbar's search icon expands into this field and its trailing x
 * collapses it back. It keeps the toolbar's height so the page content doesn't move.
 */
@Composable
fun PageSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = MaterialTheme.spacings.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            hint = stringResource(CoreR.string.search),
            leadingContent = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_search),
                    contentDescription = null,
                )
            },
            trailingContent = {
                AppIconButton(
                    onClick = onClose,
                    size = AppButtonSize.Decorator,
                    variant = AppButtonVariant.Ghost,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_x),
                            contentDescription = stringResource(CoreR.string.cancel),
                        )
                    },
                )
            },
        )
    }
}
