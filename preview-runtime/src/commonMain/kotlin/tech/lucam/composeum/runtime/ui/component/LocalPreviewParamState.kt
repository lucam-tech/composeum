package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import tech.lucam.composeum.runtime.PreviewParamState

/**
 * Provides the current [PreviewParamState] to the composition tree.
 * Set by [tech.lucam.composeum.runtime.ui.screen.PreviewDetailScreen] so that
 * composable lambdas can read the active param values.
 *
 * Uses [staticCompositionLocalOf] because [PreviewParamState] changes are always
 * accompanied by an explicit [androidx.compose.runtime.CompositionLocalProvider] at the
 * call site, so the dynamic invalidation of [androidx.compose.runtime.compositionLocalOf]
 * is unnecessary — and its default factory would produce a new instance on every access,
 * making it an unstable key for [androidx.compose.runtime.remember].
 */
val LocalPreviewParamState = staticCompositionLocalOf { PreviewParamState() }
