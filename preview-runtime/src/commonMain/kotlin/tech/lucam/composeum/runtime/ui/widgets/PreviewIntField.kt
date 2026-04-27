package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Slider widget for an Int or Long preview parameter.
 *
 * @param label       Label displayed above the slider showing the current value.
 * @param value       Current integer value.
 * @param onValue     Called with the new value when the slider moves.
 * @param range       Inclusive slider range; defaults to 0..100.
 * @param description Optional helper text shown below the slider.
 */
@Composable
fun PreviewIntField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    range: IntRange = 0..100,
    description: String = "",
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValue(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
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
