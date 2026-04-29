package tech.lucam.composeum.runtime.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tech.lucam.composeum.runtime.PreviewParamState
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.withCustomTypeDefaults
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.ui.component.LocalPreviewConfig
import tech.lucam.composeum.runtime.ui.component.LocalPreviewParamState
import tech.lucam.composeum.runtime.ui.component.ParamPanel
import tech.lucam.composeum.runtime.ui.component.PreviewRenderer

/**
 * Displays a single preview entry full-width with its param panel below.
 *
 * The upper area renders the composable inside a [PreviewRenderer] with the
 * current [tech.lucam.composeum.runtime.PreviewParamState] provided via
 * [LocalPreviewParamState]. The lower area shows a [ParamPanel] with an inline
 * reset button if the entry has a param form; it is hidden when
 * [tech.lucam.composeum.runtime.PreviewEntry.paramForm] is null.
 *
 * @param entryKey         Key of the entry to display.
 * @param registry         Source of all preview entries.
 * @param config           Browser configuration.
 * @param modifier         Modifier applied to the root column.
 */
@Composable
fun PreviewDetailScreen(
    entryKey: String,
    registry: PreviewRegistry,
    config: PreviewConfig,
    modifier: Modifier = Modifier,
) {
    val entry = remember(registry.entries, entryKey) {
        registry.entries.find { it.key == entryKey }
    }

    if (entry == null) {
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

    val activeConfig = LocalPreviewConfig.current
    val previewOverride = activeConfig.previewOverrides[entry.key]
    val groupConfig = activeConfig.groupOverrides[entry.group::class]
    val effectivePreviewWrapper = previewOverride?.previewWrapper ?: groupConfig?.previewWrapper ?: activeConfig.previewWrapper
    val showParamPanel = previewOverride?.showParamPanel ?: activeConfig.showParamPanel
    val hasParams = entry.paramForm != null && showParamPanel

    var paramState by remember(entryKey) {
        mutableStateOf(entry.paramDefaults.toInitialState().withCustomTypeDefaults(entry, activeConfig))
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalPreviewParamState provides paramState) {
                if (effectivePreviewWrapper != null) {
                    effectivePreviewWrapper(entry) {
                        PreviewRenderer(entry = entry, modifier = Modifier.wrapContentSize())
                    }
                } else {
                    PreviewRenderer(entry = entry, modifier = Modifier.wrapContentSize())
                }
            }
        }

        if (hasParams) {
            ParamPanel(
                entry = entry,
                paramState = paramState,
                onParamStateChange = { paramState = it },
                onReset = {
                    paramState = entry.paramDefaults.toInitialState()
                        .withCustomTypeDefaults(entry, activeConfig)
                },
            )
        }
    }
}
