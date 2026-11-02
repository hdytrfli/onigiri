package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.models.FindroidItemPerson
import dev.hdytrfli.onigiri.ui.theme.spacings

@Composable
fun InfoText(
    genres: List<String>,
    director: FindroidItemPerson?,
    writers: List<FindroidItemPerson>,
) {
    val genresLabel = stringResource(CoreR.string.genres)
    val directorLabel = stringResource(CoreR.string.director)
    val writersLabel = stringResource(CoreR.string.writers)
    InfoRows(
        rows =
            listOfNotNull(
                if (genres.isNotEmpty()) genresLabel to genres.joinToString(", ") else null,
                director?.let { directorLabel to it.name },
                if (writers.isNotEmpty()) {
                    writersLabel to writers.joinToString(", ") { it.name }
                } else {
                    null
                },
            )
    )
}

/** Generic label/value metadata rows, shared by every detail page. */
@Composable
fun InfoRows(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        rows.forEach { (label, value) ->
            if (value.isNotBlank()) {
                InfoRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
