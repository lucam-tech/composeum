package tech.lucam.composeum.runtime.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Displays a composable preview error with an expandable stack trace. */
@Composable
internal fun PreviewErrorCard(throwable: Throwable, modifier: Modifier = Modifier) {
    var stackExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = throwable::class.simpleName ?: "Error",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = throwable.message ?: "(no message)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = { stackExpanded = !stackExpanded }) {
                Text(if (stackExpanded) "Hide stack trace" else "Show stack trace")
            }
            AnimatedVisibility(visible = stackExpanded) {
                Text(
                    text = throwable.stackTraceToString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .height(200.dp),
                )
            }
        }
    }
}
