package tech.lucam.composeum.sample.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.annotation.ComposePreview
import tech.lucam.composeum.annotation.PreviewParam
import tech.lucam.composeum.sample.SampleGroup

enum class SampleSize { Small, Medium, Large }

@ComposePreview(
    name = "Greeting Card",
    group = SampleGroup.Components::class,
    description = "A simple greeting with no parameters.",
)
@Composable
fun GreetingPreview() {
    Text(
        text = "Hello, Compose Preview!",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(
    name = "Android Preview",
    group = "Android Preview"
)
@Composable
fun AndroidPreview() {
    Text(
        text = "Android Preview",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(16.dp),
    )
}


@Preview(
    name = "Android Preview 2",
    group = "Android Preview"
)
@Composable
fun AndroidPreview2() {
    Text(
        text = "Android Preview 2",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(16.dp),
    )
}

@PreviewLightDark
@Composable
fun AndroidPreviewLightDark() {
    Text(
        text = "Android PreviewLightDark",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(16.dp),
    )
}


@ComposePreview(
    name = "Primary Button",
    group = SampleGroup.Components::class,
    description = "A button whose label is controlled via a string param.",
    tags = ["button", "cta"],
)
@Composable
fun PrimaryButtonPreview(
    @PreviewParam(label = "Label", default = "Click me") label: String = "Click me",
) {
    Button(onClick = {}) {
        Text(text = label)
    }
}

@ComposePreview(
    name = "Toggle Row",
    group = SampleGroup.Components::class,
    description = "A switch whose enabled state is controlled via a boolean param.",
)
@Composable
fun ToggleRowPreview(
    @PreviewParam(label = "Enabled", default = "true") enabled: Boolean = true,
) {
    Switch(checked = enabled, onCheckedChange = {})
}

@ComposePreview(
    name = "Sized Text",
    group = SampleGroup.Screens::class,
    description = "Text that changes style based on a SampleSize enum param.",
)
@Composable
fun SizedTextPreview(
    @PreviewParam(label = "Size", default = "Medium") size: SampleSize = SampleSize.Medium,
) {
    val style = when (size) {
        SampleSize.Small  -> MaterialTheme.typography.bodySmall
        SampleSize.Medium -> MaterialTheme.typography.bodyLarge
        SampleSize.Large  -> MaterialTheme.typography.headlineMedium
    }
    Text(
        text = "Size: $size",
        style = style,
        modifier = Modifier.padding(16.dp),
    )
}

@ComposePreview(
    name = "Profile Card",
    group = SampleGroup.Components::class,
    description = "A card showing a user profile with an online status indicator.",
    tags = ["card", "profile"],
)
@Composable
fun ProfileCardPreview(
    @PreviewParam(label = "Username", default = "Jane Doe") username: String = "Jane Doe",
    @PreviewParam(label = "Online", default = "true") isOnline: Boolean = true,
) {
    Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = username.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = username, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOnline) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@ComposePreview(
    name = "Alert Banner",
    group = SampleGroup.Screens::class,
    description = "A coloured banner whose appearance is driven by a severity dropdown.",
    tags = ["banner", "alert"],
)
@Composable
fun AlertBannerPreview(
    @PreviewParam(
        label = "Severity",
        default = "Info",
        options = ["Info", "Warning", "Error"],
    ) severity: String = "Info",
) {
    val containerColor = when (severity) {
        "Warning" -> MaterialTheme.colorScheme.tertiaryContainer
        "Error"   -> MaterialTheme.colorScheme.errorContainer
        else      -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (severity) {
        "Warning" -> MaterialTheme.colorScheme.onTertiaryContainer
        "Error"   -> MaterialTheme.colorScheme.onErrorContainer
        else      -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "[$severity] This is an alert message.",
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@ComposePreview(
    name = "Color Swatch",
    group = SampleGroup.Components::class,
    description = "A square swatch demonstrating the colour-picker param widget.",
)
@Composable
fun ColorSwatchPreview(
    // default encoded as ARGB Long decimal: 0xFF0000FF (pure blue) = 4278190335
    @PreviewParam(label = "Swatch Color", default = "4278190335") color: Color = Color(red = 0f, green = 0f, blue = 1f),
) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
    )
}
