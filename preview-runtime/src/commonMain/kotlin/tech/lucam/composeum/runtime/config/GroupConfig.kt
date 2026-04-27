package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Immutable

/**
 * Per-group configuration override applied to a specific [tech.lucam.composeum.annotation.PreviewGroup].
 *
 * Register overrides via [PreviewConfigBuilder.groups]. Null fields inherit the global [PreviewConfig] value.
 */
@Immutable
data class GroupConfig(
    /** Override the number of thumbnail columns for this group; null inherits [PreviewConfig.thumbnailColumns]. */
    val thumbnailColumns: Int? = null,
    /** Override how this leaf group behaves when tapped; null inherits [PreviewConfig.groupExpansionMode]. */
    val expansionMode: GroupExpansionMode? = null,
    /** Override the composable that wraps this group's preview list; null inherits [PreviewConfig.groupWrapper]. */
    val groupWrapper: GroupWrapper? = null,
    /** Override the composable that wraps each preview card in this group; null inherits [PreviewConfig.previewWrapper]. */
    val previewWrapper: PreviewWrapper? = null,
)
