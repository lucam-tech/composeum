package tech.lucam.composeum.runtime

import tech.lucam.composeum.runtime.config.PreviewConfig

/** Registry that holds a collection of [PreviewEntry] instances. */
interface PreviewRegistry {
    /** All preview entries provided by this registry, in registration order. */
    val entries: List<PreviewEntry>

    /** Optional registry-local browser configuration and overrides. */
    val config: PreviewConfig
        get() = PreviewConfig()
}
