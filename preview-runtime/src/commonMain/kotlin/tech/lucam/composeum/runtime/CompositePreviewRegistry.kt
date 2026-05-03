package tech.lucam.composeum.runtime

import tech.lucam.composeum.runtime.config.PreviewConfigOverride
import tech.lucam.composeum.runtime.config.asOverride
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

    override val configOverride: PreviewConfigOverride =
        registries
            .map { it.config.asOverride().mergedWith(it.configOverride) }
            .fold(PreviewConfigOverride()) { acc, next -> acc.mergedWith(next) }
}
