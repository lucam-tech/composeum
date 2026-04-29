package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.runtime.PreviewPaddingValues

/**
 * Composite widget for a [PreviewPaddingValues] preview parameter.
 *
 * Renders four independent [Dp] sliders (top, bottom, start, end) under a shared label.
 *
 * @param label       Label displayed above the four sliders.
 * @param value       Current [PreviewPaddingValues].
 * @param onValue     Called with the updated [PreviewPaddingValues] whenever any slider moves.
 * @param range       Inclusive slider range in raw float dp; defaults to 0..128.
 * @param description Optional helper text shown below the sliders.
 */
@Composable
fun PreviewPaddingValuesField(
    label: String,
    value: PreviewPaddingValues,
    onValue: (PreviewPaddingValues) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..128f,
    description: String = "",
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        PreviewDpField(
            label = "Top",
            value = value.top,
            range = range,
            onValue = { onValue(value.copy(top = it)) },
        )
        PreviewDpField(
            label = "Bottom",
            value = value.bottom,
            range = range,
            onValue = { onValue(value.copy(bottom = it)) },
        )
        PreviewDpField(
            label = "Start",
            value = value.start,
            range = range,
            onValue = { onValue(value.copy(start = it)) },
        )
        PreviewDpField(
            label = "End",
            value = value.end,
            range = range,
            onValue = { onValue(value.copy(end = it)) },
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
