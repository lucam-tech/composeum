package tech.lucam.composeum.runtime.ui.screen

import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry

/**
 * Android actual: infers the sealed-class hierarchy via JVM reflection so that
 * nested sealed interfaces (e.g. `Themed > Dark`, `Themed > Accented`) appear
 * as collapsible parent nodes in the group list.
 */
internal actual fun buildGroupTree(allEntries: List<PreviewEntry>): List<GroupNode> {
    if (allEntries.isEmpty()) return emptyList()

    val entriesByGroup: Map<PreviewGroup, List<PreviewEntry>> = allEntries.groupBy { it.group }
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

    val allParentClasses: Set<Class<*>> = allGroupClasses
        .flatMap { it.previewGroupAncestors() }
        .toSet()

    // Classes that are supertypes of ALL groups are invisible root containers.
    val commonAncestors: Set<Class<*>> = allParentClasses
        .filter { ancestor -> allGroupClasses.all { ancestor.isAssignableFrom(it) } }
        .toSet()

    val syntheticParentClasses: Set<Class<*>> = allParentClasses - commonAncestors

    data class MutableNode(
        val key: String,
        val name: String,
        val description: String,
        val entries: List<PreviewEntry>,
        val children: MutableList<MutableNode> = mutableListOf(),
    )

    val nodeMap: MutableMap<Class<*>, MutableNode> = LinkedHashMap()

    for ((group, groupEntries) in entriesByGroup) {
        nodeMap[group::class.java] = MutableNode(
            key = group::class.qualifiedName ?: group::class.java.name,
            name = group.name,
            description = group.description,
            entries = groupEntries,
        )
    }

    for (cls in syntheticParentClasses) {
        if (cls !in nodeMap) {
            nodeMap[cls] = MutableNode(
                key = cls.name,
                name = cls.simpleName ?: cls.name,
                description = "",
                entries = emptyList(),
            )
        }
    }

    val rootNodes = mutableListOf<MutableNode>()
    for ((cls, node) in nodeMap) {
        val parentNode = cls.previewGroupAncestors()
            .firstOrNull { it !in commonAncestors && it in nodeMap }
            ?.let { nodeMap[it] }

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
