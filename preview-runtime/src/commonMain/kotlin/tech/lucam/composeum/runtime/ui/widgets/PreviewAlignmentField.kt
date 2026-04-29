package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val GRID_OPTIONS: List<Pair<String, Alignment>> = listOf(
    "Top Start"    to Alignment.TopStart,
    "Top Center"   to Alignment.TopCenter,
    "Top End"      to Alignment.TopEnd,
    "Center Start" to Alignment.CenterStart,
    "Center"       to Alignment.Center,
    "Center End"   to Alignment.CenterEnd,
    "Bottom Start" to Alignment.BottomStart,
    "Bottom Center" to Alignment.BottomCenter,
    "Bottom End"   to Alignment.BottomEnd,
)

/**
 * 3×3 grid picker for a 2D [Alignment] preview parameter.
 *
 * Each cell represents one of the nine standard alignments. The selected cell is
 * highlighted with the primary colour; tapping any cell fires [onValue].
 *
 * @param label       Label displayed above the grid.
 * @param value       Currently selected [Alignment].
 * @param onValue     Called with the newly selected [Alignment] when a cell is tapped.
 * @param description Optional helper text shown below the grid.
 */
@Composable
fun PreviewAlignmentField(
    label: String,
    value: Alignment,
    onValue: (Alignment) -> Unit,
    description: String = "",
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(0.6f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (row in 0..2) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..2) {
                        val (name, alignment) = GRID_OPTIONS[row * 3 + col]
                        val isSelected = alignment == value
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(4.dp),
                                    ) else Modifier
                                )
                                .clickable { onValue(alignment) }
                                .semantics { contentDescription = "$label: $name" },
                        )
                    }
                }
            }
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
