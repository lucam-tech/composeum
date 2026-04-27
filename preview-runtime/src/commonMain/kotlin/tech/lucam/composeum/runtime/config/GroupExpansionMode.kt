package tech.lucam.composeum.runtime.config

/** Controls how a leaf group behaves when tapped in the group list. */
enum class GroupExpansionMode {
    /** Navigate to a dedicated PreviewListScreen for the group (default). */
    SUBSCREEN,

    /** Expand inline to reveal the component list for quick access without leaving the group list. */
    INLINE,
}
