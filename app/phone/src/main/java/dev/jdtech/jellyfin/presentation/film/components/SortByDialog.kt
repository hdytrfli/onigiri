package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import dev.jdtech.jellyfin.presentation.components.AppOptionRow
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.drawableRes
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
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

    val orderValues = SortOrder.entries
    val orderNames = stringArrayResource(CoreR.array.sort_order_options)
    val orderOptions = orderValues.zip(orderNames)

    var selectedOption by remember { mutableStateOf(currentSortBy) }
    var selectedOrder by remember { mutableStateOf(currentSortOrder) }

    val lazyListState = rememberLazyListState()

    BaseDialog(title = stringResource(CoreR.string.sort_by), onDismiss = onDismissRequest) {
        contentPadding ->
        Column {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(contentPadding).fillMaxWidth()
            ) {
                orderOptions.forEachIndexed { index, order ->
                    SegmentedButton(
                        selected = order.first == selectedOrder,
                        onClick = {
                            selectedOrder = order.first
                            onUpdate(selectedOption, selectedOrder)
                        },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = orderOptions.size,
                            ),
                        colors =
                            SegmentedButtonDefaults.colors(
                                inactiveContainerColor = Color.Transparent
                            ),
                        icon = {
                            AnimatedVisibility(
                                visible = order.first == selectedOrder,
                                enter = fadeIn(),
                                exit = ExitTransition.None,
                            ) {
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_check),
                                    contentDescription = null,
                                )
                            }
                        },
                        label = { Text(order.second) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {
                items(options) { option ->
                    AppOptionRow(
                        text = option.second,
                        onClick = {
                            selectedOption = option.first
                            onUpdate(selectedOption, selectedOrder)
                        },
                        selected = option.first == selectedOption,
                        role = Role.RadioButton,
                        iconDrawableId = OptionIcon.Sort.drawableRes(),
                    )
                }
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
