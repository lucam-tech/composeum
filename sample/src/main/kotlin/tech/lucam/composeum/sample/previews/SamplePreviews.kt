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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.annotation.ComposePreview
import tech.lucam.composeum.annotation.PreviewParam
import tech.lucam.composeum.annotation.PreviewTag
import tech.lucam.composeum.sample.SampleGroup

enum class InboxFilter { All, Unread, Starred }

sealed interface SampleTag : PreviewTag {
    data object Button : SampleTag {
        override val title: String = "button"
    }

    data object Cta : SampleTag {
        override val title: String = "cta"
    }

    data object Card : SampleTag {
        override val title: String = "card"
    }

    data object Profile : SampleTag {
        override val title: String = "profile"
    }
}

@ComposePreview(
    name = "Greeting Card",
    group = SampleGroup.Components::class,
    description = "A simple preview with no parameters.",
)
@Composable
fun GreetingPreview() {
    Card(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Hello from Composeum",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@ComposePreview(
    name = "Primary Button",
    group = SampleGroup.Components::class,
    description = "A button with one string-backed preview parameter.",
    tags = [SampleTag.Button::class, SampleTag.Cta::class],
)
@Composable
fun PrimaryButtonPreview(
    @PreviewParam(label = "Label") label: String = "Continue",
) {
    Card(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = {},
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = label)
        }
    }
}

@ComposePreview(
    name = "Inbox Summary",
    group = SampleGroup.Screens.Home::class,
    description = "A small screen preview with enum and boolean parameters.",
)
@Composable
fun InboxSummaryPreview(
    @PreviewParam(label = "Filter") filter: InboxFilter = InboxFilter.All,
    @PreviewParam(label = "Notifications") notificationsEnabled: Boolean = true,
) {
    Card(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Inbox",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Filter: $filter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = {},
                )
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@ComposePreview(
    name = "Profile Card",
    group = SampleGroup.Components::class,
    description = "A slightly richer component preview with two basic parameters.",
    tags = [SampleTag.Card::class, SampleTag.Profile::class],
)
@Composable
fun ProfileCardPreview(
    @PreviewParam(label = "Name") name: String = "Jane Doe",
    @PreviewParam(label = "Online") online: Boolean = true,
) {
    Card(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (online) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (online) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
