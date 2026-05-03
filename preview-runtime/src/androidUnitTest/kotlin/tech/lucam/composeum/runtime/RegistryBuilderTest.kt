package tech.lucam.composeum.runtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.annotation.PreviewVariantGroup
import tech.lucam.composeum.annotation.SimplePreviewTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RegistryBuilderTest {

    private val group = object : PreviewGroup { override val name = "TestGroup" }
    private val variantGroup = object : PreviewVariantGroup {}

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
    fun `auto-generated key is group identity slash preview name`() {
        val registry = buildRegistry {
            preview(name = "Bar", group = group) {}
        }
        assertEquals("${group.groupKey()}/Bar", registry.entries[0].key)
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
    fun `variant metadata is stored on entry`() {
        val registry = buildRegistry {
            preview(
                name = "Base",
                group = group,
                variantGroup = variantGroup,
                isDefaultVariant = true,
            ) {}
        }

        val entry = registry.entries.single()
        assertEquals(variantGroup, entry.variantGroup)
        assertTrue(entry.isDefaultVariant)
    }

    @Test
    fun `default variant requires variant group`() {
        try {
            buildRegistry {
                preview(
                    name = "Broken",
                    group = group,
                    isDefaultVariant = true,
                ) {}
            }
            fail("Expected preview registration to reject isDefaultVariant without variantGroup.")
        } catch (_: IllegalArgumentException) {
        }
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
    fun `parameterized preview auto-key uses group identity slash name`() {
        val registry = buildRegistry {
            preview(name = "P", group = group, params = previewParams {}) { _ -> }
        }
        assertEquals("${group.groupKey()}/P", registry.entries[0].key)
    }

    @Test
    fun `auto-generated keys stay distinct when groups share the same display name`() {
        val alpha = object : PreviewGroup { override val name = "Shared" }
        val beta = object : PreviewGroup { override val name = "Shared" }

        val registry = buildRegistry {
            preview(name = "Card", group = alpha) {}
            preview(name = "Card", group = beta) {}
        }

        assertEquals(2, registry.entries.size)
        assertTrue(registry.entries.map { it.key }.distinct().size == 2)
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

    // --- New predefined param types ---

    @Test
    fun `alignment param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { alignment(key = "align", default = Alignment.TopStart) },
            ) { _ -> }
        }
        assertEquals(Alignment.TopStart, registry.entries[0].paramDefaults.defaults["align"])
    }

    @Test
    fun `alignmentHorizontal param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { alignmentHorizontal(key = "h", default = Alignment.End) },
            ) { _ -> }
        }
        assertEquals(Alignment.End, registry.entries[0].paramDefaults.defaults["h"])
    }

    @Test
    fun `alignmentVertical param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { alignmentVertical(key = "v", default = Alignment.Bottom) },
            ) { _ -> }
        }
        assertEquals(Alignment.Bottom, registry.entries[0].paramDefaults.defaults["v"])
    }

    @Test
    fun `arrangementHorizontal param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { arrangementHorizontal(key = "arr", default = Arrangement.End) },
            ) { _ -> }
        }
        assertEquals(Arrangement.End, registry.entries[0].paramDefaults.defaults["arr"])
    }

    @Test
    fun `arrangementVertical param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { arrangementVertical(key = "arr", default = Arrangement.Bottom) },
            ) { _ -> }
        }
        assertEquals(Arrangement.Bottom, registry.entries[0].paramDefaults.defaults["arr"])
    }

    @Test
    fun `contentSlot param default index is reflected in stored ContentSlotValue`() {
        val options: List<Pair<String, @Composable () -> Unit>> = listOf("None" to {}, "Icon" to {})
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { contentSlot(key = "slot", options = options, defaultIndex = 1) },
            ) { _ -> }
        }
        val stored = registry.entries[0].paramDefaults.defaults["slot"] as? ContentSlotValue
        assertNotNull(stored)
        assertEquals(1, stored!!.selectedIndex)
        assertEquals("Icon", stored.selectedName)
    }

    @Test
    fun `contentSlot requires non-empty options`() {
        val empty: List<Pair<String, @Composable () -> Unit>> = emptyList()
        try {
            previewParams { contentSlot(key = "slot", options = empty) }
            assert(false) { "Expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("slot"))
        }
    }

    @Test
    fun `fontWeight param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { fontWeight(key = "fw", default = FontWeight.Bold) },
            ) { _ -> }
        }
        assertEquals(FontWeight.Bold, registry.entries[0].paramDefaults.defaults["fw"])
    }

    @Test
    fun `textAlign param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { textAlign(key = "ta", default = TextAlign.Center) },
            ) { _ -> }
        }
        assertEquals(TextAlign.Center, registry.entries[0].paramDefaults.defaults["ta"])
    }

    @Test
    fun `shape param default is stored as Dp in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { shape(key = "r", default = 12.dp) },
            ) { _ -> }
        }
        assertEquals(12.dp, registry.entries[0].paramDefaults.defaults["r"])
    }

    @Test
    fun `contentScale param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { contentScale(key = "cs", default = ContentScale.Crop) },
            ) { _ -> }
        }
        assertEquals(ContentScale.Crop, registry.entries[0].paramDefaults.defaults["cs"])
    }

    @Test
    fun `layoutDirection param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { layoutDirection(key = "ld", default = LayoutDirection.Rtl) },
            ) { _ -> }
        }
        assertEquals(LayoutDirection.Rtl, registry.entries[0].paramDefaults.defaults["ld"])
    }

    @Test
    fun `paddingValues param default is stored in paramDefaults`() {
        val default = PreviewPaddingValues(top = 8.dp, bottom = 16.dp, start = 4.dp, end = 4.dp)
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { paddingValues(key = "pad", default = default) },
            ) { _ -> }
        }
        assertEquals(default, registry.entries[0].paramDefaults.defaults["pad"])
    }

    @Test
    fun `fontFamily param default is stored in paramDefaults`() {
        val registry = buildRegistry {
            preview(
                name = "T", group = group,
                params = previewParams { fontFamily(key = "ff", default = FontFamily.Serif) },
            ) { _ -> }
        }
        assertEquals(FontFamily.Serif, registry.entries[0].paramDefaults.defaults["ff"])
    }
}
