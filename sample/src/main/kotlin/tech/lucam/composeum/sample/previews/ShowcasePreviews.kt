package tech.lucam.composeum.sample.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.annotation.ComposePreview
import tech.lucam.composeum.annotation.PreviewParam
import tech.lucam.composeum.sample.SampleGroup

// ── SampleGroup.Showcase ──────────────────────────────────────────────────────
// These previews appear in a single-column grid (thumbnailColumns = 1 set via
// per-group config) and are bordered by a custom previewWrapper.  They are
// intentionally full-width so they look good in the wide layout.

@ComposePreview(
    name = "Hero Section",
    group = SampleGroup.Showcase::class,
    description = "Large title + subtitle — demonstrates two String params in a wide layout.",
    tags = ["hero", "typography"],
)
@Composable
fun HeroSectionPreview(
    @PreviewParam(label = "Title", default = "Welcome Back") title: String = "Welcome Back",
    @PreviewParam(label = "Subtitle", default = "Your dashboard is up to date.") subtitle: String = "Your dashboard is up to date.",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@ComposePreview(
    name = "Progress Card",
    group = SampleGroup.Showcase::class,
    description = "Label + linear progress bar — Float param for completion percentage.",
    tags = ["progress"],
)
@Composable
fun ProgressCardPreview(
    @PreviewParam(label = "Label", default = "Upload complete") label: String = "Upload complete",
    @PreviewParam(label = "Progress", default = "0.75") progress: Float = 0.75f,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${(clamped * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { clamped },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@ComposePreview(
    name = "Step Indicator",
    group = SampleGroup.Showcase::class,
    description = "Numbered step row — Int param for current step, options-dropdown for total.",
    tags = ["stepper", "navigation"],
)
@Composable
fun StepIndicatorPreview(
    @PreviewParam(label = "Current Step", default = "2") currentStep: Int = 2,
    @PreviewParam(
        label = "Total Steps",
        default = "4",
        options = ["3", "4", "5"],
    ) totalLabel: String = "4",
) {
    val total = totalLabel.toIntOrNull()?.coerceIn(1, 8) ?: 4
    val step  = currentStep.coerceIn(1, total)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val done    = index < step - 1
            val current = index == step - 1

            Box(
                modifier = Modifier
                    .size(if (current) 36.dp else 28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            done    -> MaterialTheme.colorScheme.primary
                            current -> MaterialTheme.colorScheme.primaryContainer
                            else    -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        done    -> MaterialTheme.colorScheme.onPrimary
                        current -> MaterialTheme.colorScheme.onPrimaryContainer
                        else    -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (index < total - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (done) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }
    }
}

@ComposePreview(
    name = "Stat Row",
    group = SampleGroup.Showcase::class,
    description = "Three-column stat summary — demonstrates label and count params.",
    tags = ["stats", "dashboard"],
)
@Composable
fun StatRowPreview(
    @PreviewParam(label = "Metric", default = "Downloads") metric: String = "Downloads",
    @PreviewParam(label = "Count", default = "1284") count: Int = 1284,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell(label = metric, value = count.toString())
        StatCell(label = "Active",  value = (count / 3).toString())
        StatCell(label = "Pending", value = (count / 10).toString())
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
