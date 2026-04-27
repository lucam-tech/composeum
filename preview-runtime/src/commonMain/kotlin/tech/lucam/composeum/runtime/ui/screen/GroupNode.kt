package tech.lucam.composeum.runtime.ui.screen

import tech.lucam.composeum.runtime.PreviewEntry

internal data class GroupNode(
    val key: String,
    val name: String,
    val description: String,
    val entries: List<PreviewEntry>,
    val children: List<GroupNode>,
)

internal val GroupNode.totalCount: Int
    get() = entries.size + children.sumOf { it.totalCount }

internal val GroupNode.isLeaf: Boolean
    get() = children.isEmpty()

/**
 * Builds a tree of [GroupNode]s from a flat list of [PreviewEntry]s.
 *
 * On Android the full sealed-class hierarchy is inferred via JVM reflection so
 * that nested sealed interfaces appear as parent nodes.  On other targets a flat
 * list is produced — each group appears as an independent root node.
 */
internal expect fun buildGroupTree(allEntries: List<PreviewEntry>): List<GroupNode>
