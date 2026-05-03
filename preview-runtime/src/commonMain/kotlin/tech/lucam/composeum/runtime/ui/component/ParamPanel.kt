package tech.lucam.composeum.runtime.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamState
import tech.lucam.composeum.runtime.config.PreviewParamForm

/**
 * A collapsible panel that renders the param form for a [PreviewEntry].
 *
 * When [entry.paramForm][PreviewEntry.paramForm] is null, the panel shows a
 * "No parameters" message. Otherwise it renders the form inside a scrollable
 * column that can be collapsed via the header toggle.
 *
 * @param entry              The preview entry whose param form is rendered.
 * @param paramState         The current param state passed into the form.
 * @param onParamStateChange Called when the form updates any param value.
 * @param onReset            Called when the user taps the reset button; shown only when non-null.
 * @param modifier           Modifier applied to the root surface.
 */
@Composable
fun ParamPanel(
    entry: PreviewEntry,
    paramForm: PreviewParamForm?,
    paramState: PreviewParamState,
    onParamStateChange: (PreviewParamState) -> Unit,
    onReset: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Column {
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Parameters",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (onReset != null) {
                    IconButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset parameters",
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                        else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (expanded) "Collapse parameters" else "Expand parameters",
                    )
                }
            }

            if (expanded) {
                if (paramForm == null) {
                    Text(
                        text = "No parameters",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    CrashIsolatedParamForm(
                        entry = entry,
                        paramForm = paramForm,
                        paramState = paramState,
                        onParamStateChange = onParamStateChange,
                    )
                }
            }
        }
    }
}

/**
 * Renders [entry]'s param form inside a [SubcomposeLayout] sub-composition so that any
 * exception thrown during form composition cannot corrupt the parent composition's slot table.
 * On error the form is replaced with a short error message; the error clears when
 * [paramState] changes so the user can recover by adjusting a different parameter.
 */
@Composable
private fun CrashIsolatedParamForm(
    entry: PreviewEntry,
    paramForm: PreviewParamForm,
    paramState: PreviewParamState,
    onParamStateChange: (PreviewParamState) -> Unit,
) {
    var formError by remember(entry.key, paramState) { mutableStateOf<Throwable?>(null) }

    if (formError != null) {
        Text(
            text = "Param form error: ${formError!!.message}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        return
    }

    val errorChannel = remember(entry.key, paramState) { Channel<Throwable>(Channel.CONFLATED) }
    LaunchedEffect(entry.key, paramState) {
        val t = errorChannel.receive()
        formError = t.cause ?: t
    }

    SubcomposeLayout(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) { constraints ->
        val measurables = try {
            subcompose("form") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    paramForm.invoke(paramState, onParamStateChange)
                }
            }
        } catch (t: Throwable) {
            errorChannel.trySend(t)
            return@SubcomposeLayout layout(constraints.maxWidth, 0) {}
        }
        val placeables = measurables.map { it.measure(constraints) }
        val height = placeables.sumOf { it.height }
        layout(constraints.maxWidth, height) {
            var y = 0
            placeables.forEach { p -> p.place(0, y); y += p.height }
        }
    }
}
