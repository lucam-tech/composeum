package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Generic option-picker widget that renders a [FlowRow] of [FilterChip]s.
 *
 * Used for all enum-like preview parameters: [Alignment.Horizontal], [Alignment.Vertical],
 * [Arrangement.Horizontal], [Arrangement.Vertical], [FontWeight], [TextAlign],
 * [ContentScale], [LayoutDirection], and [FontFamily].
 *
 * @param T           The type of the value being selected.
 * @param label       Label displayed above the chips.
 * @param value       The currently selected value.
 * @param options     All selectable (name, value) pairs.
 * @param onValue     Called with the newly selected value when a chip is tapped.
 * @param description Optional helper text shown below the chips.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> PreviewOptionChipsField(
    label: String,
    value: T,
    options: List<Pair<String, T>>,
    onValue: (T) -> Unit,
    description: String = "",
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            options.forEach { (name, option) ->
                FilterChip(
                    selected = option == value,
                    onClick = { onValue(option) },
                    label = { Text(name) },
                    modifier = Modifier
                        .padding(end = 6.dp, bottom = 4.dp)
                        .semantics { contentDescription = "$label: $name" },
                )
            }
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
