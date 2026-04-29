package tech.lucam.composeum.runtime

import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.mergedWith

/**
 * Merges multiple [PreviewRegistry] instances into one.
 * Entries with duplicate [PreviewEntry.key] values are deduplicated — first occurrence wins.
 *
 * Typical usage with the KSP-generated registry and a hand-built DSL registry:
 * ```kotlin
 * CompositePreviewRegistry(GeneratedPreviewRegistry, MyDslRegistry)
 * ```
 */
class CompositePreviewRegistry(
    registries: List<PreviewRegistry>,
) : PreviewRegistry {

    /** Convenience vararg constructor — equivalent to passing a list. */
    constructor(vararg registries: PreviewRegistry) : this(registries.toList())

    override val entries: List<PreviewEntry> =
        registries
            .flatMap { it.entries }
            .distinctBy { it.key }

    override val config: PreviewConfig =
        registries
            .map { it.config }
            .fold(PreviewConfig()) { acc, next -> acc.mergedWith(next) }
}
