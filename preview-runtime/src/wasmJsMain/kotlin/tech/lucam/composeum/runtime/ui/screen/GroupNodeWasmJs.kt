package tech.lucam.composeum.runtime.ui.screen

import tech.lucam.composeum.runtime.PreviewEntry

/**
 * wasmJs actual: produces a flat list where every group is a root node.
 * Sealed-class hierarchy inference requires JVM reflection and is not available
 * on the web target; all groups are shown at the top level instead.
 */
internal actual fun buildGroupTree(allEntries: List<PreviewEntry>): List<GroupNode> {
    if (allEntries.isEmpty()) return emptyList()
    return allEntries
        .groupBy { it.group }
        .map { (group, entries) ->
            GroupNode(
                key = group::class.qualifiedName ?: group.name,
                name = group.name,
                description = group.description,
                entries = entries,
                children = emptyList(),
            )
        }
        .sortedBy { it.name }
}
