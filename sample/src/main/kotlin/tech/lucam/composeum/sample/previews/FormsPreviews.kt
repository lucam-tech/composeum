package tech.lucam.composeum.sample.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.annotation.ComposePreview
import tech.lucam.composeum.annotation.PreviewParam
import tech.lucam.composeum.sample.SampleGroup

@ComposePreview(
    name = "Slider Demo",
    group = SampleGroup.Forms::class,
    description = "A progress slider driven by a Float param (range 0.0–1.0).",
)
@Composable
fun SliderDemoPreview(
    @PreviewParam(label = "Progress", default = "0.4") progress: Float = 0.4f,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Progress: ${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@ComposePreview(
    name = "Counter",
    group = SampleGroup.Forms::class,
    description = "A circular badge displaying an integer count param.",
)
@Composable
fun CounterPreview(
    @PreviewParam(label = "Count", default = "5") count: Int = 5,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.coerceAtLeast(0).toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Text(
            text = "items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@ComposePreview(
    name = "Search Field",
    group = SampleGroup.Forms::class,
    description = "An outlined text field with a String query param.",
)
@Composable
fun SearchFieldPreview(
    @PreviewParam(label = "Query", default = "Compose") query: String = "Compose",
) {
    OutlinedTextField(
        value = query,
        onValueChange = {},
        label = { Text("Search") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}

@ComposePreview(
    name = "Tech Chip",
    group = SampleGroup.Forms::class,
    description = "An assist chip demonstrating an options-dropdown param.",
    tags = ["chip", "options"],
)
@Composable
fun TechChipPreview(
    @PreviewParam(
        label = "Technology",
        default = "Kotlin",
        options = ["Kotlin", "Compose", "Android", "Material3"],
    ) technology: String = "Kotlin",
) {
    AssistChip(
        onClick = {},
        label = { Text(technology) },
        modifier = Modifier.padding(16.dp),
    )
}
