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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Slider widget for a [TextUnit] (sp) preview parameter.
 *
 * @param label       Label displayed above the slider showing the current sp value.
 * @param value       Current [TextUnit]; non-sp units are treated as 16sp.
 * @param onValue     Called with the new sp [TextUnit] value when the slider moves.
 * @param range       Inclusive slider range in raw float sp; defaults to 8..64.
 * @param description Optional helper text shown below the slider.
 */
@Composable
fun PreviewTextUnitField(
    label: String,
    value: TextUnit,
    onValue: (TextUnit) -> Unit,
    range: ClosedFloatingPointRange<Float> = 8f..64f,
    description: String = "",
) {
    val spValue = if (value.isSp) value.value else 16f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${spValue.roundToInt()}sp",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = spValue,
            onValueChange = { onValue(it.sp) },
            valueRange = range,
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
