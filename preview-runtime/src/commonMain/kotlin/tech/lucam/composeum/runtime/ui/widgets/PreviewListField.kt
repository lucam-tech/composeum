package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Container widget for a `List<T>` preview parameter.
 * Renders a labelled section with per-item [itemContent], plus add/remove controls.
 *
 * @param label       Section label and item count shown in the header row.
 * @param itemCount   Number of items currently in the list.
 * @param onAdd       Called when the user taps the add (+) button.
 * @param onRemove    Called with the 0-based index when the user removes an item.
 * @param description Optional helper text shown below the header.
 * @param itemContent Composable slot called once per item with the item's 0-based index.
 */
@Composable
fun PreviewListField(
    label: String,
    itemCount: Int,
    onAdd: () -> Unit,
    onRemove: (index: Int) -> Unit,
    description: String = "",
    itemContent: @Composable (index: Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
            TextButton(onClick = onAdd) {
                Text("+")
            }
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        for (index in 0 until itemCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    itemContent(index)
                }
                IconButton(onClick = { onRemove(index) }) {
                    Text("×", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}
