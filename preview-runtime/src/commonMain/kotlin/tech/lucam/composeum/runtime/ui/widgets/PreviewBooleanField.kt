package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * Toggle widget for a Boolean preview parameter.
 *
 * @param label       Label displayed next to the switch.
 * @param value       Current boolean value.
 * @param onValue     Called with the new value when the switch is toggled.
 * @param description Optional helper text shown below the label.
 */
@Composable
fun PreviewBooleanField(
    label: String,
    value: Boolean,
    onValue: (Boolean) -> Unit,
    description: String = "",
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = value,
            onCheckedChange = onValue,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
