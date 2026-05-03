package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Immutable
import tech.lucam.composeum.runtime.PreviewParamDefaults

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
    /** Override the parameter form shown for this preview. */
    val paramForm: PreviewParamForm? = null,
    /** Additional defaults merged into the preview's parameter state before first render. */
    val paramDefaults: PreviewParamDefaults? = null,
)
