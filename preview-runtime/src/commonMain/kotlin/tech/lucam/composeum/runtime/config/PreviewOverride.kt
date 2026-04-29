package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Immutable

/**
 * Per-preview configuration override applied to a specific [tech.lucam.composeum.runtime.PreviewEntry.key].
 *
 * Register overrides via [PreviewConfigBuilder.preview]. Null fields inherit the resolved
 * group/global values.
 */
@Immutable
data class PreviewOverride(
    /** Override the composable that wraps this preview render; null inherits group/global wrappers. */
    val previewWrapper: PreviewWrapper? = null,
    /** Override whether the param panel is shown on the detail screen; null inherits [PreviewConfig.showParamPanel]. */
    val showParamPanel: Boolean? = null,
)
