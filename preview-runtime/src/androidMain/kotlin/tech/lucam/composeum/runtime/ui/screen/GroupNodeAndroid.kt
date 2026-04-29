package tech.lucam.composeum.runtime.ui.screen

import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry

/**
 * Android actual: builds the group tree using explicit [PreviewGroup.parent] links
 * when available, falling back to JVM reflection over sealed-class nesting for groups
 * that leave [PreviewGroup.parent] null.
 *
 * Explicit parent always wins over any inferred sealed nesting.
 */
internal actual fun buildGroupTree(allEntries: List<PreviewEntry>): List<GroupNode> {
    if (allEntries.isEmpty()) return emptyList()

    val entriesByGroup: Map<PreviewGroup, List<PreviewEntry>> = allEntries.groupBy { it.group }

    // Walk the full explicit parent chain for every group so that synthetic nodes are
    // created for all ancestor groups that have no previews of their own.
    val explicitAncestors: Set<PreviewGroup> = buildSet {
        for (group in entriesByGroup.keys) {
            var ancestor: PreviewGroup? = group.parent
            while (ancestor != null) {
                add(ancestor)
                ancestor = ancestor.parent
            }
        }
    }

    // All leaf group classes for the reflection fallback path.
    val allGroupClasses: Set<Class<*>> = entriesByGroup.keys.map { it::class.java }.toSet()

    fun Class<*>.previewGroupAncestors(): List<Class<*>> {
        val result = mutableListOf<Class<*>>()
        var cls = enclosingClass
        while (cls != null) {
            if (PreviewGroup::class.java.isAssignableFrom(cls)) result.add(cls)
            cls = cls.enclosingClass
        }
        return result
    }

    val reflectionParentClasses: Set<Class<*>> = allGroupClasses
        .filter { cls ->
            // Only use reflection for groups that have no explicit parent set.
            val group = entriesByGroup.keys.first { it::class.java == cls }
            group.parent == null
        }
        .flatMap { it.previewGroupAncestors() }
        .toSet()

    val commonAncestors: Set<Class<*>> = reflectionParentClasses
        .filter { ancestor -> allGroupClasses.all { ancestor.isAssignableFrom(it) } }
        .toSet()

    val syntheticReflectionParents: Set<Class<*>> = reflectionParentClasses - commonAncestors

    data class MutableNode(
        val key: String,
        val name: String,
        val description: String,
        val entries: List<PreviewEntry>,
        val children: MutableList<MutableNode> = mutableListOf(),
    )

    // Key: identity of the group object (or Class for reflection-inferred parents).
    val nodeByGroup: MutableMap<PreviewGroup, MutableNode> = LinkedHashMap()
    val nodeByClass: MutableMap<Class<*>, MutableNode> = LinkedHashMap()

    // Ensure nodes exist for every explicit ancestor (may have no entries of their own).
    for (ancestor in explicitAncestors) {
        if (ancestor !in nodeByGroup) {
            nodeByGroup[ancestor] = MutableNode(
                key = ancestor::class.qualifiedName ?: ancestor.name,
                name = ancestor.name,
                description = ancestor.description,
                entries = emptyList(),
            )
        }
    }

    // Leaf-group nodes.
    for ((group, groupEntries) in entriesByGroup) {
        nodeByGroup.getOrPut(group) {
            MutableNode(
                key = group::class.qualifiedName ?: group::class.java.name,
                name = group.name,
                description = group.description,
                entries = groupEntries,
            )
        }.also { node ->
            // Merge entries if the node was pre-created as a parent placeholder.
            if (node.entries.isEmpty() && groupEntries.isNotEmpty()) {
                nodeByGroup[group] = node.copy(entries = groupEntries)
            }
        }
        nodeByClass[group::class.java] = nodeByGroup[group]!!
    }

    // Synthetic nodes for reflection-inferred intermediate parents.
    for (cls in syntheticReflectionParents) {
        if (cls !in nodeByClass) {
            val node = MutableNode(
                key = cls.name,
                name = cls.simpleName ?: cls.name,
                description = "",
                entries = emptyList(),
            )
            nodeByClass[cls] = node
        }
    }

    val rootNodes = mutableListOf<MutableNode>()

    // Wire up children using explicit parent first, reflection fallback second.
    for ((group, node) in nodeByGroup) {
        val explicitParent = group.parent
        val parentNode: MutableNode? = when {
            explicitParent != null -> nodeByGroup[explicitParent]
            else -> {
                val cls = group::class.java
                cls.previewGroupAncestors()
                    .firstOrNull { it !in commonAncestors && it in nodeByClass }
                    ?.let { nodeByClass[it] }
            }
        }
        if (parentNode != null) parentNode.children.add(node) else rootNodes.add(node)
    }

    // Wire up reflection-inferred intermediate parent nodes.
    for ((cls, node) in nodeByClass) {
        if (nodeByGroup.values.any { it === node }) continue  // already handled above
        val parentNode = cls.previewGroupAncestors()
            .firstOrNull { it !in commonAncestors && it in nodeByClass }
            ?.let { nodeByClass[it] }
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
