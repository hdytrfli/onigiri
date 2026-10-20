package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * A page's toolbar: a horizontally scrolling row of pill tabs. The selected tab is the light
 * primary pill, the others are the grey secondary pill.
 */
@Composable
fun PageToolbar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        itemsIndexed(items = tabs) { index, title ->
            AppButton(
                onClick = { onSelect(index) },
                variant =
                    if (index == selectedIndex) {
                        AppButtonVariant.Primary
                    } else {
                        AppButtonVariant.Secondary
                    },
                content = { Text(text = title) },
            )
        }
    }
}
