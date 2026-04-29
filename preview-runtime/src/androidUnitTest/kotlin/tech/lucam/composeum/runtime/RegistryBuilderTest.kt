package tech.lucam.composeum.runtime

import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.annotation.SimplePreviewTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistryBuilderTest {

    private val group = object : PreviewGroup { override val name = "TestGroup" }

    // --- buildRegistry structure ---

    @Test
    fun `empty block yields empty entries`() {
        val registry = buildRegistry {}
        assertTrue(registry.entries.isEmpty())
    }

    @Test
    fun `simple preview is added to entries`() {
        val registry = buildRegistry {
            preview(name = "MyPreview", group = group) {}
        }
        assertEquals(1, registry.entries.size)
    }

    @Test
    fun `simple preview has correct name and group`() {
        val registry = buildRegistry {
            preview(name = "Foo", group = group) {}
        }
        val entry = registry.entries[0]
        assertEquals("Foo", entry.name)
        assertEquals(group, entry.group)
    }

    @Test
    fun `auto-generated key is group name slash preview name`() {
        val registry = buildRegistry {
            preview(name = "Bar", group = group) {}
        }
        assertEquals("TestGroup/Bar", registry.entries[0].key)
    }

    @Test
    fun `explicit key overrides auto-generated key`() {
        val registry = buildRegistry {
            preview(name = "Baz", group = group, key = "custom-key") {}
        }
        assertEquals("custom-key", registry.entries[0].key)
    }

    @Test
    fun `simple preview has null paramForm`() {
        val registry = buildRegistry {
            preview(name = "Simple", group = group) {}
        }
        assertNull(registry.entries[0].paramForm)
    }

    @Test
    fun `simple preview has empty paramDefaults`() {
        val registry = buildRegistry {
            preview(name = "Simple", group = group) {}
        }
        assertTrue(registry.entries[0].paramDefaults.defaults.isEmpty())
    }

    @Test
    fun `description and tags are stored on entry`() {
        val registry = buildRegistry {
            preview(
                name = "Tagged",
                group = group,
                description = "A desc",
                tags = listOf(SimplePreviewTag("a"), SimplePreviewTag("b")),
            ) {}
        }
        val entry = registry.entries[0]
        assertEquals("A desc", entry.description)
        assertEquals(listOf("a", "b"), entry.tags.map { it.title })
    }

    @Test
    fun `multiple previews are added in order`() {
        val registry = buildRegistry {
            preview(name = "First", group = group) {}
            preview(name = "Second", group = group) {}
            preview(name = "Third", group = group) {}
        }
        assertEquals(listOf("First", "Second", "Third"), registry.entries.map { it.name })
    }

    @Test
    fun `two calls to buildRegistry produce independent registries`() {
        val a = buildRegistry { preview(name = "A", group = group) {} }
        val b = buildRegistry { preview(name = "B", group = group) {} }
        assertEquals(1, a.entries.size)
        assertEquals(1, b.entries.size)
        assertEquals("A", a.entries[0].name)
        assertEquals("B", b.entries[0].name)
    }

    // --- Parameterized preview ---

    @Test
    fun `parameterized preview has non-null paramForm`() {
        val registry = buildRegistry {
            preview(
                name = "Param",
                group = group,
                params = previewParams { string(key = "k", default = "v") },
            ) { _ -> }
        }
        assertNotNull(registry.entries[0].paramForm)
    }

    @Test
    fun `parameterized preview with no params has non-null paramForm and empty defaults`() {
        val registry = buildRegistry {
            preview(name = "Empty", group = group, params = previewParams {}) { _ -> }
        }
        assertNotNull(registry.entries[0].paramForm)
        assertTrue(registry.entries[0].paramDefaults.defaults.isEmpty())
    }

    @Test
    fun `parameterized preview auto-key uses group slash name`() {
        val registry = buildRegistry {
            preview(name = "P", group = group, params = previewParams {}) { _ -> }
        }
        assertEquals("TestGroup/P", registry.entries[0].key)
    }

    // --- previewParams defaults ---

    @Test
    fun `string param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { string(key = "label", default = "Hello") },
            ) { _ -> }
        }
        assertEquals("Hello", registry.entries[0].paramDefaults.defaults["label"])
    }

    @Test
    fun `boolean param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { boolean(key = "flag", default = true) },
            ) { _ -> }
        }
        assertEquals(true, registry.entries[0].paramDefaults.defaults["flag"])
    }

    @Test
    fun `int param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { int(key = "count", default = 42) },
            ) { _ -> }
        }
        assertEquals(42, registry.entries[0].paramDefaults.defaults["count"])
    }

    @Test
    fun `float param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { float(key = "alpha", default = 0.75f) },
            ) { _ -> }
        }
        assertEquals(0.75f, registry.entries[0].paramDefaults.defaults["alpha"])
    }

    @Test
    fun `dropdown param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams {
                    dropdown(
                        key = "size",
                        options = listOf("S", "M", "L"),
                        default = "M",
                    )
                },
            ) { _ -> }
        }
        assertEquals("M", registry.entries[0].paramDefaults.defaults["size"])
    }

    @Test
    fun `multiple params all appear in defaults map`() {
        val registry = buildRegistry {
            preview(
                name = "Multi",
                group = group,
                params = previewParams {
                    string(key = "a", default = "x")
                    boolean(key = "b", default = false)
                    int(key = "c", default = 7)
                },
            ) { _ -> }
        }
        val defaults = registry.entries[0].paramDefaults.defaults
        assertEquals(3, defaults.size)
        assertEquals("x", defaults["a"])
        assertEquals(false, defaults["b"])
        assertEquals(7, defaults["c"])
    }

    @Test
    fun `paramDefaults toInitialState reflects all param defaults`() {
        val registry = buildRegistry {
            preview(
                name = "Init",
                group = group,
                params = previewParams {
                    string(key = "title", default = "Hi")
                    boolean(key = "active", default = true)
                },
            ) { _ -> }
        }
        val initialState = registry.entries[0].paramDefaults.toInitialState()
        assertEquals("Hi", initialState["title"])
        assertEquals(true, initialState["active"])
    }
}
