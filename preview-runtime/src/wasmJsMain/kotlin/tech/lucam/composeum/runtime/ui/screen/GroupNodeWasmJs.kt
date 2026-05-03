package tech.lucam.composeum.runtime.ui.screen

import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.groupKey

/**
 * wasmJs actual: builds the group tree using explicit [PreviewGroup.parent] links.
 *
 * Groups that leave [PreviewGroup.parent] null appear as root nodes (flat list,
 * same as the previous behaviour). Groups that set [PreviewGroup.parent] are nested
 * under their declared parent, enabling a hierarchy on the web target without
 * requiring JVM reflection.
 */
internal actual fun buildGroupTree(allEntries: List<PreviewEntry>): List<GroupNode> {
    if (allEntries.isEmpty()) return emptyList()

    val entriesByGroup: Map<PreviewGroup, List<PreviewEntry>> = allEntries.groupBy { it.group }

    data class MutableNode(
        val key: String,
        val name: String,
        val description: String,
        val entries: List<PreviewEntry>,
        val children: MutableList<MutableNode> = mutableListOf(),
    )

    val nodeByGroup: MutableMap<PreviewGroup, MutableNode> = LinkedHashMap()

    // Ensure a node exists for every explicit parent (may have no entries of their own).
    for (group in entriesByGroup.keys) {
        var ancestor: PreviewGroup? = group.parent
        while (ancestor != null) {
            if (ancestor !in nodeByGroup) {
                nodeByGroup[ancestor] = MutableNode(
                    key = ancestor.groupKey(),
                    name = ancestor.name,
                    description = ancestor.description,
                    entries = emptyList(),
                )
            }
            ancestor = ancestor.parent
        }
    }

    // Create nodes for leaf groups.
    for ((group, groupEntries) in entriesByGroup) {
        val existing = nodeByGroup[group]
        nodeByGroup[group] = existing?.copy(entries = existing.entries + groupEntries)
            ?: MutableNode(
                key = group.groupKey(),
                name = group.name,
                description = group.description,
                entries = groupEntries,
            )
    }

    val rootNodes = mutableListOf<MutableNode>()
    for ((group, node) in nodeByGroup) {
        val parentNode = group.parent?.let { nodeByGroup[it] }
        if (parentNode != null) parentNode.children.add(node) else rootNodes.add(node)
    }

    fun MutableNode.toGroupNode(): GroupNode = GroupNode(
        key = key,
        name = name,
        description = description,
        entries = entries,
        children = children.sortedBy { it.name }.map { it.toGroupNode() },
    )

    return rootNodes.sortedBy { it.name }.map { it.toGroupNode() }
}
