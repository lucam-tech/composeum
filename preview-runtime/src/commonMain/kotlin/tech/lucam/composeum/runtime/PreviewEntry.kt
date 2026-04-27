package tech.lucam.composeum.runtime

import androidx.compose.runtime.Composable
import tech.lucam.composeum.annotation.PreviewGroup

/**
 * A single registered composable preview.
 *
 * Instances are created either by the KSP-generated registry or by [buildRegistry].
 * The [composable] lambda is crash-isolated by [tech.lucam.composeum.runtime.ui.component.PreviewRenderer].
 */
data class PreviewEntry(
    /** Unique key used for deduplication and navigation. Generated as `"${group.name}/$name"` by default. */
    val key: String,
    /** Human-readable display name shown in the browser list. */
    val name: String,
    /** Group this preview belongs to, used to build the group tree. */
    val group: PreviewGroup,
    /** Optional subtitle shown on the detail screen. */
    val description: String,
    /** Searchable tags shown as chips on thumbnail cards. */
    val tags: List<String>,
    /** The composable to render. May read [tech.lucam.composeum.runtime.ui.component.LocalPreviewParamState]. */
    val composable: @Composable () -> Unit,
    /**
     * Composable that renders the interactive param form, or null when there are no params.
     * Receives the current [PreviewParamState] and an update callback.
     */
    val paramForm: (@Composable (PreviewParamState, (PreviewParamState) -> Unit) -> Unit)?,
    /** Default values used to construct the initial [PreviewParamState] for this entry. */
    val paramDefaults: PreviewParamDefaults,
    /** Absolute path to the source file that declares this preview, captured by KSP. Empty when created manually. */
    val sourceFile: String = "",
    /** Line number of the annotated function declaration, captured by KSP. 0 when not available. */
    val sourceLine: Int = 0,
    /**
     * Maps each state key that holds a custom (non-primitive) type to its fully-qualified type name.
     * Populated by the KSP-generated registry for params that require a
     * [tech.lucam.composeum.runtime.config.CustomParamField] registration.
     *
     * Used by [tech.lucam.composeum.runtime.ui.screen.PreviewDetailScreen] to seed the
     * initial [PreviewParamState] with runtime-provided [tech.lucam.composeum.runtime.config.CustomParamField.initialValue]
     * values from [tech.lucam.composeum.runtime.config.PreviewConfig.customTypeFields].
     */
    val customTypeParamKeys: Map<String, String> = emptyMap(),
)
