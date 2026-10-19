package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import dev.jdtech.jellyfin.presentation.components.AppOptionRow
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.drawableRes
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.presentation.models.OptionIcon

@Composable
fun SortByDialog(
    currentSortBy: SortBy,
    currentSortOrder: SortOrder,
    onUpdate: (sortBy: SortBy, sortOrder: SortOrder) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val optionValues = SortBy.entries
    val optionNames = stringArrayResource(CoreR.array.sort_by_options)
    val options = optionValues.zip(optionNames)

    var selectedOption by remember { mutableStateOf(currentSortBy) }
    var selectedOrder by remember { mutableStateOf(currentSortOrder) }

    val lazyListState = rememberLazyListState()

    BaseDialog(
        title = stringResource(CoreR.string.sort_by),
        description = stringResource(CoreR.string.sort_by_summary),
        onDismiss = onDismissRequest,
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {
            items(options) { option ->
                val isSelected = option.first == selectedOption
                AppOptionRow(
                    text = option.second,
                    onClick = {
                        if (isSelected) {
                            selectedOrder =
                                if (selectedOrder == SortOrder.ASCENDING) {
                                    SortOrder.DESCENDING
                                } else {
                                    SortOrder.ASCENDING
                                }
                        } else {
                            selectedOption = option.first
                        }
                        onUpdate(selectedOption, selectedOrder)
                    },
                    selected = isSelected,
                    iconDrawableId = OptionIcon.Sort.drawableRes(),
                    role = Role.RadioButton,
                    trailingContent = {
                        if (isSelected) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (selectedOrder == SortOrder.ASCENDING) {
                                            CoreR.drawable.ic_move_up
                                        } else {
                                            CoreR.drawable.ic_move_down
                                        }
                                    ),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun SortByDialogPreview() {
    FindroidTheme {
        SortByDialog(
            currentSortBy = SortBy.NAME,
            currentSortOrder = SortOrder.ASCENDING,
            onUpdate = { _, _ -> },
            onDismissRequest = {},
        )
    }
}
