package dev.hdytrfli.onigiri.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.ui.theme.spacings

/** One selectable language code/display-name pair. */
data class LanguageOption(val code: String, val label: String)

/** Stable defaults used alongside the user's own language. */
val DefaultSuggestedLanguageCodes = listOf("eng", "spa", "fra", "deu", "jpn")

/**
 * Shared language picker used by settings and subtitle search: a suggested section followed by the
 * complete catalog. An optional "not set" row is kept above the suggestions.
 */
@Composable
fun LanguagePickerDialog(
    title: String,
    description: String?,
    options: List<LanguageOption>,
    selectedCode: String?,
    suggestedLabel: String,
    availableLabel: String,
    suggestedCodes: List<String> = emptyList(),
    notSetLabel: String? = null,
    @DrawableRes optionIcon: Int? = null,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val optionsByCode = options.associateBy { it.code }
    val suggested = suggestedCodes.mapNotNull(optionsByCode::get).distinctBy { it.code }.take(6)
    val suggestedCodeSet = suggested.mapTo(mutableSetOf()) { it.code }
    val remaining = options.filterNot { it.code in suggestedCodeSet }

    BaseDialog(
        title = title,
        description = description,
        onDismiss = onDismiss,
    ) { _ ->
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            if (suggested.isNotEmpty()) {
                item(key = "suggested-header") { LanguageHeader(suggestedLabel) }
                items(suggested, key = { "suggested-${it.code}" }) { option ->
                    LanguageRow(
                        option = option,
                        selected = option.code == selectedCode,
                        icon = optionIcon,
                        onSelect = onSelect,
                    )
                }
            }

            item(key = "available-header") { LanguageHeader(availableLabel) }
            if (notSetLabel != null) {
                item(key = "not-set") {
                    AppOptionRow(
                        text = notSetLabel,
                        onClick = { onSelect(null) },
                        selected = selectedCode == null,
                        iconDrawableId = optionIcon,
                        role = Role.RadioButton,
                    )
                }
            }
            items(remaining, key = { it.code }) { option ->
                LanguageRow(
                    option = option,
                    selected = option.code == selectedCode,
                    icon = optionIcon,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun LanguageHeader(label: String) {
    Text(
        text = label,
        modifier =
            Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = MaterialTheme.spacings.small,
                bottom = MaterialTheme.spacings.extraSmall,
            ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LanguageRow(
    option: LanguageOption,
    selected: Boolean,
    @DrawableRes icon: Int?,
    onSelect: (String?) -> Unit,
) {
    AppOptionRow(
        text = option.label,
        onClick = { onSelect(option.code) },
        selected = selected,
        iconDrawableId = icon,
        role = Role.RadioButton,
    )
}
