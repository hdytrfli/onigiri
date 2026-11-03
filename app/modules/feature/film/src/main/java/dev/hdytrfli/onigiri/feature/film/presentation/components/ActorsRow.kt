package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.models.FindroidItemPerson
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import java.util.UUID

@Composable
fun ActorsRow(
    actors: List<FindroidItemPerson>,
    onActorClick: (personId: UUID) -> Unit,
    contentPadding: PaddingValues,
    @StringRes titleRes: Int = CoreR.string.cast_amp_crew,
) {
    Column(modifier = Modifier.padding(contentPadding)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
    }
    BoxWithConstraints {
        val columns = gridColumns(GridSize.AVATAR).coerceAtLeast(1)
        val gap = LocalGridGap.current
        val layoutDirection = LocalLayoutDirection.current
        val availableWidth =
            (maxWidth -
                    contentPadding.calculateStartPadding(layoutDirection) -
                    contentPadding.calculateEndPadding(layoutDirection) -
                    gap * (columns - 1).toFloat())
                .coerceAtLeast(0.dp)
        val itemWidth = (availableWidth / columns.toFloat()).coerceAtLeast(72.dp)

        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(items = actors) { person ->
                PersonItem(
                    person = person,
                    width = itemWidth,
                    onClick = { onActorClick(person.id) },
                )
            }
        }
    }
}
