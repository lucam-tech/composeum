package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Wraps a param widget with a null-toggle switch.
 * When [isNull] is true the inner [content] is hidden and the parameter receives null.
 *
 * @param label        Label displayed next to the null toggle.
 * @param isNull       Whether the parameter is currently null.
 * @param onNullChange Called with the new null state when the toggle changes.
 * @param description  Optional helper text shown below the label.
 * @param content      The inner widget displayed when the parameter is non-null.
 */
@Composable
fun PreviewNullableWrapper(
    label: String,
    isNull: Boolean,
    onNullChange: (Boolean) -> Unit,
    description: String = "",
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "$label (nullable)", style = MaterialTheme.typography.labelMedium)
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (isNull) "null" else "non-null",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            Switch(
                checked = !isNull,
                onCheckedChange = { onNullChange(!it) },
                modifier = Modifier.semantics { contentDescription = "$label null toggle" },
            )
        }
        if (!isNull) {
            content()
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }
    }
}
