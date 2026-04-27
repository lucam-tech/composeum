package tech.lucam.composeum.runtime.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.runtime.util.tryOpenInIde
import kotlinx.coroutines.launch

@Composable
internal fun SourceLocationDialog(
    sourceFile: String,
    sourceLine: Int,
    /** Precomputed web URL; non-null only when [PreviewConfig.sourceBaseUrl] is configured. */
    webUrl: String?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var ideLoading by remember { mutableStateOf(false) }
    var ideError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Source file") },
        text = {
            Column {
                Text(
                    text = "$sourceFile:$sourceLine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(12.dp))

                SourceOptionRow(
                    label = "Open in IDE",
                    enabled = !ideLoading,
                    leadingIcon = {
                        if (ideLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Code, contentDescription = null)
                        }
                    },
                    onClick = {
                        ideLoading = true
                        ideError = false
                        scope.launch {
                            val success = tryOpenInIde(sourceFile, sourceLine)
                            if (success) {
                                onDismiss()
                            } else {
                                ideLoading = false
                                ideError = true
                            }
                        }
                    },
                )
                if (ideError) {
                    Text(
                        text = "Couldn't reach the IDE. This works on Android emulators when the IDE is running on the same machine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 40.dp, bottom = 4.dp),
                    )
                }

                if (webUrl != null) {
                    SourceOptionRow(
                        label = "Open in browser",
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        },
                        onClick = {
                            try {
                                uriHandler.openUri(webUrl)
                            } catch (_: Exception) {
                            }
                            onDismiss()
                        },
                    )
                }

                SourceOptionRow(
                    label = "Copy path",
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        clipboardManager.setText(AnnotatedString("$sourceFile:$sourceLine"))
                        onDismiss()
                    },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SourceOptionRow(
    label: String,
    leadingIcon: @Composable () -> Unit,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        leadingIcon()
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
