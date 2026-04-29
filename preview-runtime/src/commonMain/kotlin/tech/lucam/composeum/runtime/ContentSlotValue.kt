package tech.lucam.composeum.runtime

import androidx.compose.runtime.Composable

/**
 * Holds the user-defined list of named composable options for a content slot parameter,
 * together with the index of the currently selected option.
 *
 * Instances are stored in [PreviewParamState] by the [tech.lucam.composeum.runtime.PreviewParamsDsl.contentSlot]
 * DSL method. The composable references are captured at registry-build time and are
 * in-memory only — they are never serialized or persisted across sessions.
 *
 * @param options       Named composable options provided by the caller.
 * @param selectedIndex Index into [options] of the currently active composable.
 */
class ContentSlotValue internal constructor(
    internal val options: List<Pair<String, @Composable () -> Unit>>,
    val selectedIndex: Int,
) {
    /** The composable currently selected in the slot. */
    val content: @Composable () -> Unit
        get() = options[selectedIndex].second

    /** Display name of the currently selected option. */
    val selectedName: String
        get() = options[selectedIndex].first

    /** All option display names. */
    val optionNames: List<String>
        get() = options.map { it.first }
}
