package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.compositionLocalOf
import tech.lucam.composeum.runtime.config.PreviewConfig

/**
 * Provides the active [PreviewConfig] to the composition tree.
 * Set by [tech.lucam.composeum.runtime.ui.ComposeumBrowser] so that widgets such as
 * [tech.lucam.composeum.runtime.ui.widgets.PreviewCustomTypeField] can look up
 * registered [tech.lucam.composeum.runtime.config.CustomParamField] instances.
 */
val LocalPreviewConfig = compositionLocalOf { PreviewConfig() }
