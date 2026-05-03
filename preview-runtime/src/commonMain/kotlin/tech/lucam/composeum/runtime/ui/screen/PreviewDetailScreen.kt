package tech.lucam.composeum.runtime.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.runtime.PreviewParamState
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.families
import tech.lucam.composeum.runtime.withCustomTypeDefaults
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.ui.component.LocalPreviewParamState
import tech.lucam.composeum.runtime.ui.component.LocalPreviewRenderContext
import tech.lucam.composeum.runtime.ui.component.ParamPanel
import tech.lucam.composeum.runtime.ui.component.PreviewRenderContext
import tech.lucam.composeum.runtime.ui.component.PreviewRenderer

private val DETAIL_CONTEXT = PreviewRenderContext(isThumbnail = false)

/**
 * Displays one preview family full-width with its param panel below.
 *
 * The upper area renders the composable inside a [PreviewRenderer] with the
 * current [tech.lucam.composeum.runtime.PreviewParamState] provided via
 * [LocalPreviewParamState]. The lower area shows a [ParamPanel] with an inline
 * reset button if the entry has a param form; it is hidden when
 * [tech.lucam.composeum.runtime.PreviewEntry.paramForm] is null.
 *
 * @param familyKey        Key of the preview family to display.
 * @param registry         Source of all preview entries.
 * @param config           Browser configuration.
 * @param onActiveEntryChanged Called whenever the active flavor changes.
 * @param modifier         Modifier applied to the root column.
 */
@Composable
fun PreviewDetailScreen(
    familyKey: String,
    registry: PreviewRegistry,
    config: PreviewConfig,
    initialSelectedEntryKey: String? = null,
    initialParamState: PreviewParamState? = null,
    onActiveEntryChanged: (PreviewEntry?) -> Unit = {},
    onParamStateChanged: (PreviewEntry, PreviewParamState) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val family = remember(registry.entries, familyKey) {
        registry.families().find { it.key == familyKey }
    }

    if (family == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Preview not found.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    var selectedEntryKey by remember(familyKey, initialSelectedEntryKey) {
        mutableStateOf(
            initialSelectedEntryKey?.takeIf { key -> family.entries.any { it.key == key } }
                ?: family.defaultEntry.key,
        )
    }
    val activeConfig = config
    val activeEntry = family.entries.firstOrNull { it.key == selectedEntryKey } ?: family.defaultEntry
    val previewOverride = activeConfig.previewOverrides[activeEntry.key]
    val groupConfig = activeConfig.groupOverrides[activeEntry.group::class]
    val effectivePreviewWrapper = previewOverride?.previewWrapper ?: groupConfig?.previewWrapper ?: activeConfig.previewWrapper
    val showParamPanel = previewOverride?.showParamPanel ?: activeConfig.showParamPanel
    val effectiveParamForm = previewOverride?.paramForm ?: activeEntry.paramForm
    val hasParams = effectiveParamForm != null && showParamPanel
    val paramStates = remember(familyKey, initialSelectedEntryKey, initialParamState) {
        mutableStateMapOf<String, PreviewParamState>().apply {
            if (initialSelectedEntryKey != null && initialParamState != null) {
                this[initialSelectedEntryKey] = initialParamState
            }
        }
    }

    fun initialState(entry: PreviewEntry): PreviewParamState {
        val overrideDefaults = activeConfig.previewOverrides[entry.key]?.paramDefaults?.defaults.orEmpty()
        return tech.lucam.composeum.runtime.PreviewParamDefaults(
            defaults = entry.paramDefaults.defaults + overrideDefaults,
        ).toInitialState().withCustomTypeDefaults(entry, activeConfig)
    }

    val paramState = paramStates.getOrPut(activeEntry.key) { initialState(activeEntry) }

    LaunchedEffect(activeEntry.key) {
        onActiveEntryChanged(activeEntry)
    }

    LaunchedEffect(activeEntry.key, paramState) {
        onParamStateChanged(activeEntry, paramState)
    }

    var selectorExpanded by remember(familyKey) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        if (family.entries.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Box {
                    TextButton(onClick = { selectorExpanded = true }) {
                        Text(activeEntry.name)
                    }
                    DropdownMenu(
                        expanded = selectorExpanded,
                        onDismissRequest = { selectorExpanded = false },
                    ) {
                        family.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.name) },
                                onClick = {
                                    selectedEntryKey = entry.key
                                    selectorExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalPreviewParamState provides paramState,
                LocalPreviewRenderContext provides DETAIL_CONTEXT,
            ) {
                if (effectivePreviewWrapper != null) {
                    effectivePreviewWrapper(activeEntry) {
                        PreviewRenderer(entry = activeEntry, modifier = Modifier.wrapContentSize())
                    }
                } else {
                    PreviewRenderer(entry = activeEntry, modifier = Modifier.wrapContentSize())
                }
            }
        }

        if (hasParams) {
            ParamPanel(
                entry = activeEntry,
                paramForm = effectiveParamForm,
                paramState = paramState,
                onParamStateChange = { paramStates[activeEntry.key] = it },
                onReset = {
                    paramStates[activeEntry.key] = initialState(activeEntry)
                },
            )
        }
    }
}
