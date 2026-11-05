package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.models.SortOrder
import dev.hdytrfli.onigiri.ui.components.AppOptionRow
import dev.hdytrfli.onigiri.ui.components.BaseDialog

@Composable
fun MusicSortDialog(
    options: List<Pair<MusicSortField, String>>,
    currentSortBy: MusicSortField,
    currentSortOrder: SortOrder,
    onUpdate: (MusicSortField, SortOrder) -> Unit,
    onDismissRequest: () -> Unit,
) {
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
                    role = Role.RadioButton,
                    trailingContent = {
                        if (isSelected) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (selectedOrder == SortOrder.ASCENDING) {
                                            CoreR.drawable.ic_chevron_up
                                        } else {
                                            CoreR.drawable.ic_chevron_down
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

@Composable
fun MusicDisplayDialog(
    options: List<Pair<MusicDisplayField, String>>,
    selected: Set<MusicDisplayField>,
    onToggle: (MusicDisplayField) -> Unit,
    onDismiss: () -> Unit,
) {
    BaseDialog(
        title = stringResource(CoreR.string.display_info),
        description = stringResource(CoreR.string.display_info_summary),
        onDismiss = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            options.forEach { (field, label) ->
                AppOptionRow(
                    text = label,
                    onClick = { onToggle(field) },
                    selected = field in selected,
                    role = Role.Checkbox,
                )
            }
        }
    }
}
