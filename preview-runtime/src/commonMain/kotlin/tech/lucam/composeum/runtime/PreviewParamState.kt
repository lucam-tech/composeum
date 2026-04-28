package tech.lucam.composeum.runtime

import androidx.compose.runtime.Immutable
import tech.lucam.composeum.runtime.config.PreviewConfig
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap

/** Immutable snapshot of current parameter values for a preview. */
@Immutable
data class PreviewParamState(
    val values: PersistentMap<String, Any> = persistentMapOf(),
) {
    /** Returns a new state with [key] set to [value]. */
    fun put(key: String, value: Any): PreviewParamState =
        copy(values = values.put(key, value))

    /** Returns the typed value currently stored under [key], or `null` when absent. */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? = values[key] as? T
}

/** Default parameter values used to construct the initial [PreviewParamState]. */
data class PreviewParamDefaults(
    val defaults: Map<String, Any>,
) {
    /** Converts the defaults map into an initial [PreviewParamState]. */
    fun toInitialState(): PreviewParamState =
        PreviewParamState(values = defaults.toPersistentMap())
}

/**
 * Returns a new [PreviewParamState] with runtime-provided initial values for any
 * [PreviewEntry.customTypeParamKeys] that have a matching [PreviewConfig.customTypeFields]
 * registration. Keys already present in state are not overwritten (persisted user edits win).
 */
internal fun PreviewParamState.withCustomTypeDefaults(
    entry: PreviewEntry,
    config: PreviewConfig,
): PreviewParamState {
    if (entry.customTypeParamKeys.isEmpty()) return this
    var result = this
    for ((key, typeName) in entry.customTypeParamKeys) {
        val initial = config.customTypeFields[typeName]?.initialValue ?: continue
        result = result.put(key, initial)
    }
    return result
}
