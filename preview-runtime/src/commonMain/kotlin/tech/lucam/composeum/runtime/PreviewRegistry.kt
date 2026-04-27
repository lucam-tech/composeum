package tech.lucam.composeum.runtime

/** Registry that holds a collection of [PreviewEntry] instances. */
interface PreviewRegistry {
    /** All preview entries provided by this registry, in registration order. */
    val entries: List<PreviewEntry>
}
