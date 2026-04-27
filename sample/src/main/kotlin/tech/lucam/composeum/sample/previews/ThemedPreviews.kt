package tech.lucam.composeum.sample.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.annotation.ComposePreview
import tech.lucam.composeum.annotation.PreviewParam
import tech.lucam.composeum.sample.SampleGroup

// ── SampleGroup.Themed.Dark ───────────────────────────────────────────────────
// These previews are rendered against a dark backdrop (set via groupWrapper in
// MainActivity).  They are good for spot-checking components in dark-mode contexts.

@ComposePreview(
    name = "Star Rating",
    group = SampleGroup.Themed.Dark::class,
    description = "Five-star rating indicator — useful for checking icon colour on dark surfaces.",
    tags = ["rating", "icon"],
)
@Composable
fun StarRatingPreview(
    @PreviewParam(label = "Stars", default = "3") stars: Int = 3,
) {
    Row(modifier = Modifier.padding(12.dp)) {
        repeat(5) { index ->
            val filled = index < stars.coerceIn(0, 5)
            Text(
                text = if (filled) "★" else "☆",
                style = MaterialTheme.typography.headlineMedium,
                color = if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@ComposePreview(
    name = "Mood Chip",
    group = SampleGroup.Themed.Dark::class,
    description = "Coloured pill whose hue reflects mood — demonstrates options-dropdown param.",
    tags = ["chip", "status"],
)
@Composable
fun MoodChipPreview(
    @PreviewParam(
        label = "Mood",
        default = "Happy",
        options = ["Happy", "Neutral", "Sad"],
    ) mood: String = "Happy",
) {
    val containerColor = when (mood) {
        "Happy"   -> MaterialTheme.colorScheme.primary
        "Sad"     -> MaterialTheme.colorScheme.error
        else      -> MaterialTheme.colorScheme.secondary
    }
    val contentColor = when (mood) {
        "Happy"   -> MaterialTheme.colorScheme.onPrimary
        "Sad"     -> MaterialTheme.colorScheme.onError
        else      -> MaterialTheme.colorScheme.onSecondary
    }
    Surface(
        color = containerColor,
        shape = CircleShape,
        modifier = Modifier.padding(12.dp),
    ) {
        Text(
            text = mood,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@ComposePreview(
    name = "Online Indicator",
    group = SampleGroup.Themed.Dark::class,
    description = "Dot + label status indicator — simple Boolean param.",
)
@Composable
fun OnlineIndicatorPreview(
    @PreviewParam(label = "Online", default = "true") isOnline: Boolean = true,
) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isOnline) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ── SampleGroup.Themed.Accented ───────────────────────────────────────────────
// These previews appear against a primary-container background (set via
// groupWrapper), so they are ideal for verifying brand-colour contrast.

@ComposePreview(
    name = "Callout Banner",
    group = SampleGroup.Themed.Accented::class,
    description = "Inline callout with a dot leader — String param for message text.",
    tags = ["banner", "callout"],
)
@Composable
fun CalloutBannerPreview(
    @PreviewParam(label = "Message", default = "New feature available!") message: String = "New feature available!",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@ComposePreview(
    name = "Category Tag",
    group = SampleGroup.Themed.Accented::class,
    description = "Assist chip with an options-dropdown to switch technology category.",
    tags = ["chip", "options"],
)
@Composable
fun CategoryTagPreview(
    @PreviewParam(
        label = "Category",
        default = "Android",
        options = ["Android", "iOS", "Web", "Backend"],
    ) category: String = "Android",
) {
    AssistChip(
        onClick = {},
        label = { Text(category) },
        modifier = Modifier.padding(12.dp),
    )
}
