package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Color picker widget showing a swatch grid of Material3 color-scheme roles.
 *
 * @param label       Label displayed above the swatch grid.
 * @param value       Currently selected [Color].
 * @param onValue     Called with the newly selected [Color] when a swatch is tapped.
 * @param description Optional helper text shown below the swatches.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewColorField(
    label: String,
    value: Color,
    onValue: (Color) -> Unit,
    description: String = "",
) {
    val colorScheme = MaterialTheme.colorScheme
    val swatches = remember(colorScheme) {
        listOf(
            "Primary" to colorScheme.primary,
            "On Primary" to colorScheme.onPrimary,
            "Primary Container" to colorScheme.primaryContainer,
            "Secondary" to colorScheme.secondary,
            "On Secondary" to colorScheme.onSecondary,
            "Secondary Container" to colorScheme.secondaryContainer,
            "Tertiary" to colorScheme.tertiary,
            "On Tertiary" to colorScheme.onTertiary,
            "Tertiary Container" to colorScheme.tertiaryContainer,
            "Error" to colorScheme.error,
            "Error Container" to colorScheme.errorContainer,
            "Background" to colorScheme.background,
            "Surface" to colorScheme.surface,
            "Surface Variant" to colorScheme.surfaceVariant,
            "Outline" to colorScheme.outline,
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            swatches.forEach { (name, color) ->
                val isSelected = color == value
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = RoundedCornerShape(4.dp),
                            ) else Modifier
                        )
                        .clickable { onValue(color) }
                        .semantics { contentDescription = name },
                )
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
