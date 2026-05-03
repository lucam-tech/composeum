package tech.lucam.composeum.runtime.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults

// Sealed fixture for the reflection-fallback test — must be top-level or class-level.
private sealed interface SealedG : PreviewGroup {
    data object Parent : SealedG {
        override val name = "Parent"
    }

    data object Child : SealedG {
        override val name = "Child"
    }
}

class GroupNodeAndroidTest {

    // ---- helpers ----

    private fun entry(name: String, group: PreviewGroup) = PreviewEntry(
        key = "test/$name",
        name = name,
        group = group,
        description = "",
        tags = emptyList(),
        composable = @Composable { Text(name) },
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    // ---- flat / root groups ----

    @Test
    fun `single group with no parent is a root node`() {
        val g = object : PreviewGroup {
            override val name = "G"
        }
        val tree = buildGroupTree(listOf(entry("A", g)))
        assertEquals(1, tree.size)
        assertEquals("G", tree[0].name)
    }

    @Test
    fun `multiple independent groups produce multiple root nodes`() {
        val g1 = object : PreviewGroup {
            override val name = "Alpha"
        }
        val g2 = object : PreviewGroup {
            override val name = "Beta"
        }
        val tree = buildGroupTree(listOf(entry("A", g1), entry("B", g2)))
        assertEquals(2, tree.size)
        assertEquals(listOf("Alpha", "Beta"), tree.map { it.name })
    }

    // ---- explicit parent ----

    @Test
    fun `explicit parent creates a child node`() {
        val parent = object : PreviewGroup {
            override val name = "Parent"
        }
        val child = object : PreviewGroup {
            override val name = "Child"
            override val parent = parent
        }
        val tree = buildGroupTree(listOf(entry("E", child)))
        assertEquals(1, tree.size)
        assertEquals("Parent", tree[0].name)
        assertEquals(1, tree[0].children.size)
        assertEquals("Child", tree[0].children[0].name)
    }

    @Test
    fun `explicit parent with no entries of its own becomes a synthetic node`() {
        val parent = object : PreviewGroup {
            override val name = "Container"
        }
        val child = object : PreviewGroup {
            override val name = "Inner"
            override val parent = parent
        }
        val tree = buildGroupTree(listOf(entry("E", child)))
        val parentNode = tree.single()
        assertEquals("Container", parentNode.name)
        assertTrue(parentNode.entries.isEmpty())
        assertEquals(1, parentNode.children.size)
    }

    @Test
    fun `explicit parent with its own entries also shows them`() {
        val parent = object : PreviewGroup {
            override val name = "Parent"
        }
        val child = object : PreviewGroup {
            override val name = "Child"
            override val parent = parent
        }
        val tree = buildGroupTree(listOf(entry("ParentEntry", parent), entry("ChildEntry", child)))
        val parentNode = tree.single()
        assertEquals(1, parentNode.entries.size)
        assertEquals("ParentEntry", parentNode.entries[0].name)
        assertEquals(1, parentNode.children.size)
        assertEquals(1, parentNode.children[0].entries.size)
        assertEquals("ChildEntry", parentNode.children[0].entries[0].name)
    }

    @Test
    fun `two-level explicit parent chain is reflected in the tree`() {
        val root = object : PreviewGroup {
            override val name = "Root"
        }
        val mid = object : PreviewGroup {
            override val name = "Mid"
            override val parent = root
        }
        val leaf = object : PreviewGroup {
            override val name = "Leaf"
            override val parent = mid
        }
        val tree = buildGroupTree(listOf(entry("E", leaf)))
        assertEquals(1, tree.size)
        assertEquals("Root", tree[0].name)
        assertEquals(1, tree[0].children.size)
        assertEquals("Mid", tree[0].children[0].name)
        assertEquals(1, tree[0].children[0].children.size)
        assertEquals("Leaf", tree[0].children[0].children[0].name)
    }

    @Test
    fun `children are sorted by name`() {
        val parent = object : PreviewGroup {
            override val name = "Parent"
        }
        val c1 = object : PreviewGroup {
            override val name = "Zeta"
            override val parent = parent
        }
        val c2 = object : PreviewGroup {
            override val name = "Alpha"
            override val parent = parent
        }
        val c3 = object : PreviewGroup {
            override val name = "Mu"
            override val parent = parent
        }
        val tree = buildGroupTree(listOf(entry("e1", c1), entry("e2", c2), entry("e3", c3)))
        val childNames = tree.single().children.map { it.name }
        assertEquals(listOf("Alpha", "Mu", "Zeta"), childNames)
    }

    // ---- sealed-class reflection fallback ----

    @Test
    fun `sealed interface nesting is inferred via reflection`() {
        // SealedG is a common ancestor — stripped as a root container.
        // Parent and Child both surface as independent root nodes.
        val tree = buildGroupTree(listOf(entry("A", SealedG.Parent), entry("B", SealedG.Child)))
        val names = tree.map { it.name }.sorted()
        assertTrue("Both groups should appear", names.containsAll(listOf("Child", "Parent")))
    }

    // ---- empty ----

    @Test
    fun `empty entry list produces empty tree`() {
        assertTrue(buildGroupTree(emptyList()).isEmpty())
    }
}
