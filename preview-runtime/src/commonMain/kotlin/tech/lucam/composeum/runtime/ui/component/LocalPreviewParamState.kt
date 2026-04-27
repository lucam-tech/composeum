package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.compositionLocalOf
import tech.lucam.composeum.runtime.PreviewParamState

/**
 * Provides the current [PreviewParamState] to the composition tree.
 * Set by [tech.lucam.composeum.runtime.ui.screen.PreviewDetailScreen] so that
 * composable lambdas can read the active param values.
 */
val LocalPreviewParamState = compositionLocalOf { PreviewParamState() }
