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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Slider widget for a [Dp] preview parameter.
 *
 * @param label       Label displayed above the slider showing the current dp value.
 * @param value       Current [Dp] value.
 * @param onValue     Called with the new [Dp] value when the slider moves.
 * @param range       Inclusive slider range in raw float dp; defaults to 0..512.
 * @param description Optional helper text shown below the slider.
 */
@Composable
fun PreviewDpField(
    label: String,
    value: Dp,
    onValue: (Dp) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..512f,
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
                text = "${value.value.roundToInt()}dp",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.value,
            onValueChange = { onValue(Dp(it)) },
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
