package dev.jdtech.jellyfin.presentation.film.components

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
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun InfoText(
    genres: List<String>,
    director: FindroidItemPerson?,
    writers: List<FindroidItemPerson>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        if (genres.isNotEmpty()) {
            InfoRow(label = stringResource(CoreR.string.genres), value = genres.joinToString(", "))
        }
        director?.let { person ->
            InfoRow(label = stringResource(CoreR.string.director), value = person.name)
        }
        if (writers.isNotEmpty()) {
            InfoRow(
                label = stringResource(CoreR.string.writers),
                value = writers.joinToString(", ") { it.name },
            )
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
