package tech.lucam.composeum.sample.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.annotation.ComposePreview
import tech.lucam.composeum.annotation.PreviewParam
import tech.lucam.composeum.sample.AlertSeverity
import tech.lucam.composeum.sample.ButtonVariant
import tech.lucam.composeum.sample.ContentState
import tech.lucam.composeum.sample.SampleGroup
import tech.lucam.composeum.sample.UserProfile

// ── Enum ─────────────────────────────────────────────────────────────────────

@ComposePreview(
    name = "Button Variants",
    group = SampleGroup.ParamTypes::class,
    description = "Enum @PreviewParam — cycle through Primary, Secondary, and Destructive variants.",
    tags = ["enum", "button"],
)
@Composable
fun ButtonVariantPreview(
    @PreviewParam(label = "Variant", default = "Primary") variant: ButtonVariant = ButtonVariant.Primary,
) {
    val containerColor = when (variant) {
        ButtonVariant.Primary     -> MaterialTheme.colorScheme.primary
        ButtonVariant.Secondary   -> MaterialTheme.colorScheme.secondary
        ButtonVariant.Destructive -> MaterialTheme.colorScheme.error
    }
    Button(
        onClick = {},
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier.padding(16.dp),
    ) {
        Text("$variant Button")
    }
}

// ── Nullable ──────────────────────────────────────────────────────────────────

@ComposePreview(
    name = "Optional Subtitle",
    group = SampleGroup.ParamTypes::class,
    description = "Nullable String @PreviewParam — toggle the subtitle on/off via the null switch.",
    tags = ["nullable"],
)
@Composable
fun OptionalSubtitlePreview(
    @PreviewParam(label = "Subtitle", default = "null") subtitle: String? = null,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Main Heading", style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ── Data class ────────────────────────────────────────────────────────────────

@ComposePreview(
    name = "User Profile Card",
    group = SampleGroup.ParamTypes::class,
    description = "Data class @PreviewParam — the param panel expands into one field per constructor property.",
    tags = ["data class", "card"],
)
@Composable
fun UserProfileCardPreview(
    @PreviewParam(label = "Profile") profile: UserProfile = UserProfile(
        name = "Jane Doe",
        role = "Engineer",
        isVerified = true,
    ),
) {
    Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = profile.name, style = MaterialTheme.typography.bodyLarge)
                    if (profile.isVerified) {
                        Text(
                            text = " ✓",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    text = profile.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Sealed interface ──────────────────────────────────────────────────────────

@ComposePreview(
    name = "Content State",
    group = SampleGroup.ParamTypes::class,
    description = "Sealed interface @PreviewParam — select the active variant from a type dropdown; " +
        "each non-object variant exposes its own fields below.",
    tags = ["sealed", "state"],
)
@Composable
fun ContentStatePreview(
    @PreviewParam(label = "State", default = "Idle") state: ContentState = ContentState.Idle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ContentState.Idle -> Text(
                text = "Idle — nothing to show.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ContentState.Loading -> CircularProgressIndicator()
            is ContentState.Success -> Text(
                text = "✓ ${state.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            is ContentState.Failure -> Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "[${state.code}] ${state.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

// ── List<T> ───────────────────────────────────────────────────────────────────

@ComposePreview(
    name = "Tag Cloud",
    group = SampleGroup.ParamTypes::class,
    description = "List<String> @PreviewParam — add or remove tags dynamically from the param panel.",
    tags = ["list"],
)
@Composable
fun TagCloudPreview(
    @PreviewParam(label = "Tags", default = "Compose|Android|KMP") tags: List<String> = listOf("Compose", "Android", "KMP"),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (tags.isEmpty()) {
            Text(
                text = "No tags",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            tags.forEach { tag ->
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── Custom type ───────────────────────────────────────────────────────────────

@ComposePreview(
    name = "Alert Badge",
    group = SampleGroup.ParamTypes::class,
    description = "Custom type @PreviewParam — AlertSeverity is a plain class KSP cannot introspect; " +
        "its widget is registered via customTypeField<AlertSeverity> in MainActivity.",
    tags = ["custom type", "alert"],
)
@Composable
fun AlertBadgePreview(
    @PreviewParam(label = "Severity") severity: AlertSeverity = AlertSeverity.MEDIUM,
) {
    val (bg, fg) = when (severity.level) {
        1    -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        2    -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        3    -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
            Text(text = severity.label, color = fg, style = MaterialTheme.typography.labelLarge)
        }
    }
}
