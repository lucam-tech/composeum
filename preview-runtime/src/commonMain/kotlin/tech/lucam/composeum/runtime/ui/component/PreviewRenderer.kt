package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.store.ResolvedSettings
import kotlinx.coroutines.channels.Channel

/**
 * Provides [ResolvedSettings] to the composition tree.
 * Defaults to scale 1× with all flags matching [ResolvedSettings] defaults.
 */
val LocalResolvedSettings = compositionLocalOf { ResolvedSettings.DEFAULT }

/**
 * Renders [entry] inside a crash-isolated layout.
 *
 * Exceptions thrown during composition are caught and replaced with [PreviewErrorCard].
 * Font and UI scale from [LocalResolvedSettings] are applied via [LocalDensity].
 *
 * Crash isolation works via [SubcomposeLayout]: the preview composable runs inside a
 * sub-composition whose measure call is wrapped in try/catch. Any exception is forwarded
 * to a [Channel] and picked up by a [LaunchedEffect], which then sets the error state
 * during the composition phase to avoid writing snapshot state during layout.
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

    // Re-keying on paramState lets the user recover from an error by changing a param value.
    val paramState = LocalPreviewParamState.current
    var error by remember(entry.key, paramState) { mutableStateOf<Throwable?>(null) }

    if (error != null) {
        PreviewErrorCard(throwable = error!!, modifier = modifier)
        return
    }

    // Exceptions caught in the layout phase are forwarded here so the snapshot-state
    // write happens in the composition phase (LaunchedEffect), not the layout phase.
    // Writing snapshot state during layout would cause a layout→recompose→layout loop.
    val errorChannel = remember(entry.key, paramState) { Channel<Throwable>(Channel.CONFLATED) }

    LaunchedEffect(entry.key, paramState) {
        val t = errorChannel.receive()
        // Prefer the original cause over any Compose-runtime wrapper.
        error = t.cause ?: t
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        SubcomposeLayout(modifier = modifier) { constraints ->
            val measurables = try {
                subcompose("content") { entry.composable() }
            } catch (t: Throwable) {
                errorChannel.trySend(t)
                return@SubcomposeLayout layout(0, 0) {}
            }
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.width } ?: 0
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(width, height) { placeables.forEach { it.place(0, 0) } }
        }
    }
}
