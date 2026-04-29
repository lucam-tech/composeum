package tech.lucam.composeum.annotation

/**
 * Marker interface for organising previews into a navigable group tree.
 *
 * Groups can be arranged into a hierarchy in two ways — they may be combined freely:
 *
 * **Sealed interface nesting (existing behaviour):**
 * ```kotlin
 * sealed interface AppGroup : PreviewGroup {
 *     sealed interface Components : AppGroup {
 *         data object Buttons : Components { override val name = "Buttons" }
 *     }
 * }
 * ```
 *
 * **Explicit [parent] field (new):**
 * Groups do not need to be in the same file or form a sealed hierarchy. Any
 * [PreviewGroup] object can declare its parent directly:
 * ```kotlin
 * data object Components : PreviewGroup { override val name = "Components" }
 * data object Buttons    : PreviewGroup {
 *     override val name   = "Buttons"
 *     override val parent = Components
 * }
 * ```
 * When [parent] is non-null it takes precedence over any sealed-class nesting
 * that the runtime might otherwise infer via reflection.
 */
interface PreviewGroup {
    val name: String
    val description: String get() = ""

    /**
     * Optional explicit parent group. When set, this group appears as a child of
     * [parent] in the browser's group tree regardless of sealed-interface nesting.
     *
     * Defaults to `null`, which means the parent is inferred from the sealed
     * class hierarchy (Android) or the group is shown as a root node (wasmJs).
     */
    val parent: PreviewGroup? get() = null
}
