package tech.lucam.composeum.runtime

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.lucam.composeum.annotation.PreviewGroup

class CompositePreviewRegistryTest {

    private val group = object : PreviewGroup {
        override val name = "Test"
    }

    private fun entry(key: String, name: String = key) = PreviewEntry(
        key = key,
        name = name,
        group = group,
        description = "",
        tags = emptyList(),
        composable = {},
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    @Test
    fun `merges entries from multiple registries`() {
        val a = object : PreviewRegistry {
            override val entries = listOf(entry("a"))
        }
        val b = object : PreviewRegistry {
            override val entries = listOf(entry("b"))
        }
        val composite = CompositePreviewRegistry(listOf(a, b))
        assertEquals(listOf("a", "b"), composite.entries.map { it.key })
    }

    @Test
    fun `deduplicates by key - first occurrence wins`() {
        val first = object : PreviewRegistry {
            override val entries = listOf(entry("dup", name = "First"))
        }
        val second = object : PreviewRegistry {
            override val entries = listOf(entry("dup", name = "Second"))
        }
        val composite = CompositePreviewRegistry(listOf(first, second))
        assertEquals(1, composite.entries.size)
        assertEquals("First", composite.entries[0].name)
    }

    @Test
    fun `empty registries list yields empty entries`() {
        val composite = CompositePreviewRegistry(emptyList())
        assertEquals(emptyList<PreviewEntry>(), composite.entries)
    }

    @Test
    fun `single registry with no duplicates passes through unchanged`() {
        val entries = listOf(entry("x"), entry("y"), entry("z"))
        val registry = object : PreviewRegistry {
            override val entries = entries
        }
        val composite = CompositePreviewRegistry(listOf(registry))
        assertEquals(entries, composite.entries)
    }

    @Test
    fun `only duplicate key is deduplicated, others remain`() {
        val a = object : PreviewRegistry {
            override val entries = listOf(entry("shared", name = "First"), entry("unique-a"))
        }
        val b = object : PreviewRegistry {
            override val entries = listOf(entry("shared", name = "Second"), entry("unique-b"))
        }
        val composite = CompositePreviewRegistry(listOf(a, b))
        assertEquals(listOf("shared", "unique-a", "unique-b"), composite.entries.map { it.key })
        assertEquals("First", composite.entries[0].name)
    }
}
