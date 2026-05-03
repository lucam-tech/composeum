package tech.lucam.composeum.runtime.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.store.ResolvedSettings

/**
 * Provides [ResolvedSettings] to the composition tree.
 * Defaults to scale 1× with all flags matching [ResolvedSettings] defaults.
 */
val LocalResolvedSettings = compositionLocalOf { ResolvedSettings.DEFAULT }

/**
 * Renders [entry] with the active font/UI scale from [LocalResolvedSettings].
 *
 * Full crash isolation for arbitrary throwing composables is not currently implemented in the
 * shared runtime because Compose does not support `try/catch` around composable invocations and
 * the subcomposition-based workarounds were not stable under Robolectric unit tests.
 */
@Composable
fun PreviewRenderer(entry: PreviewEntry, modifier: Modifier = Modifier) {
    val settings = LocalResolvedSettings.current
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, settings.fontScale, settings.uiScale) {
        Density(
            density = baseDensity.density * settings.uiScale,
            fontScale = settings.fontScale,
        )
    }

    // Read the current preview param state so updates still recompose the preview content.
    LocalPreviewParamState.current

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Box(modifier = modifier) {
            entry.composable()
        }
    }
}
