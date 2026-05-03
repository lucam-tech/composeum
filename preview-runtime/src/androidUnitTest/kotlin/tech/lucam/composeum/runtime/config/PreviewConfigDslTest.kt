package tech.lucam.composeum.runtime.config

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import tech.lucam.composeum.annotation.PreviewGroup

class PreviewConfigDslTest {

    // --- Default values ---

    @Test
    fun `empty block produces default config values`() {
        val config = previewConfig {}
        assertEquals(1.0f, config.fontScale)
        assertEquals(1.0f, config.uiScale)
        assertNull(config.isDarkMode)
        assertEquals(ThemeOptionDefaults.Classic.id, config.defaultThemeId)
        assertNull(config.locale)
        assertEquals(true, config.showDescriptions)
        assertEquals(true, config.showTags)
        assertEquals(true, config.showParamPanel)
        assertEquals(2, config.thumbnailColumns)
        assertNull(config.browserWrapper)
        assertNull(config.groupWrapper)
        assertNull(config.previewWrapper)
        assertEquals(emptyMap<Any, Any>(), config.groupOverrides)
    }

    // --- Scalar fields ---

    @Test
    fun `fontScale is set correctly`() {
        val config = previewConfig { fontScale = 1.5f }
        assertEquals(1.5f, config.fontScale)
    }

    @Test
    fun `uiScale is set correctly`() {
        val config = previewConfig { uiScale = 0.75f }
        assertEquals(0.75f, config.uiScale)
    }

    @Test
    fun `isDarkMode is set correctly`() {
        val config = previewConfig { isDarkMode = true }
        assertEquals(true, config.isDarkMode)
    }

    @Test
    fun `locale is set correctly`() {
        val config = previewConfig { locale = "de-DE" }
        assertEquals("de-DE", config.locale)
    }

    @Test
    fun `defaultThemeId is set correctly`() {
        val config = previewConfig { defaultThemeId = ThemeOptionDefaults.Ocean.id }
        assertEquals(ThemeOptionDefaults.Ocean.id, config.defaultThemeId)
    }

    @Test
    fun `showDescriptions is set correctly`() {
        val config = previewConfig { showDescriptions = false }
        assertEquals(false, config.showDescriptions)
    }

    @Test
    fun `showTags is set correctly`() {
        val config = previewConfig { showTags = false }
        assertEquals(false, config.showTags)
    }

    @Test
    fun `showParamPanel is set correctly`() {
        val config = previewConfig { showParamPanel = false }
        assertEquals(false, config.showParamPanel)
    }

    @Test
    fun `thumbnailColumns is set correctly`() {
        val config = previewConfig { thumbnailColumns = 4 }
        assertEquals(4, config.thumbnailColumns)
    }

    // --- Wrapper setters ---

    @Test
    fun `browserWrapper is set when provided`() {
        val config = previewConfig { browserWrapper { content -> content() } }
        assertNotNull(config.browserWrapper)
    }

    @Test
    fun `groupWrapper is set when provided`() {
        val config = previewConfig { groupWrapper { _, content -> content() } }
        assertNotNull(config.groupWrapper)
    }

    @Test
    fun `previewWrapper is set when provided`() {
        val config = previewConfig { previewWrapper { _, content -> content() } }
        assertNotNull(config.previewWrapper)
    }

    @Test
    fun `themeOptions can be assigned directly`() {
        val customTheme = ThemeOption("brand", "Brand", Color(0xFF112233), Color(0xFF445566))
        val config = previewConfig { themeOptions = listOf(customTheme) }
        assertEquals(listOf(customTheme), config.themeOptions)
    }

    @Test
    fun `themeOption appends custom theme`() {
        val customTheme = ThemeOption("brand", "Brand", Color(0xFF112233), Color(0xFF445566))
        val config = previewConfig { themeOption(customTheme) }
        assertEquals(listOf(customTheme), config.themeOptions)
    }

    // --- Group overrides ---

    private sealed interface TestGroup : PreviewGroup {
        data object Buttons : TestGroup {
            override val name = "Buttons"
        }

        data object Cards : TestGroup {
            override val name = "Cards"
        }
    }

    @Test
    fun `group override is registered by KClass`() {
        val config = previewConfig {
            groups {
                group(TestGroup.Buttons::class) { thumbnailColumns = 3 }
            }
        }
        val override = config.groupOverrides[TestGroup.Buttons::class]
        assertNotNull(override)
        assertEquals(3, override!!.thumbnailColumns)
    }

    @Test
    fun `group override is registered via reified type`() {
        val config = previewConfig {
            groups {
                group<TestGroup.Cards> { thumbnailColumns = 1 }
            }
        }
        val override = config.groupOverrides[TestGroup.Cards::class]
        assertNotNull(override)
        assertEquals(1, override!!.thumbnailColumns)
    }

    @Test
    fun `multiple group overrides are all registered`() {
        val config = previewConfig {
            groups {
                group<TestGroup.Buttons> { thumbnailColumns = 2 }
                group<TestGroup.Cards> { thumbnailColumns = 4 }
            }
        }
        assertEquals(2, config.groupOverrides.size)
        assertEquals(2, config.groupOverrides[TestGroup.Buttons::class]!!.thumbnailColumns)
        assertEquals(4, config.groupOverrides[TestGroup.Cards::class]!!.thumbnailColumns)
    }

    @Test
    fun `group override with no fields has null thumbnailColumns`() {
        val config = previewConfig {
            groups {
                group<TestGroup.Buttons> {}
            }
        }
        assertNull(config.groupOverrides[TestGroup.Buttons::class]!!.thumbnailColumns)
    }

    @Test
    fun `GroupConfigBuilder sets groupWrapper`() {
        val config = previewConfig {
            groups {
                group<TestGroup.Buttons> {
                    groupWrapper { _, content -> content() }
                }
            }
        }
        assertNotNull(config.groupOverrides[TestGroup.Buttons::class]!!.groupWrapper)
    }

    @Test
    fun `GroupConfigBuilder sets previewWrapper`() {
        val config = previewConfig {
            groups {
                group<TestGroup.Buttons> {
                    previewWrapper { _, content -> content() }
                }
            }
        }
        assertNotNull(config.groupOverrides[TestGroup.Buttons::class]!!.previewWrapper)
    }

    // --- Immutability ---

    @Test
    fun `two calls to previewConfig produce independent configs`() {
        val a = previewConfig { thumbnailColumns = 1 }
        val b = previewConfig { thumbnailColumns = 3 }
        assertEquals(1, a.thumbnailColumns)
        assertEquals(3, b.thumbnailColumns)
    }

    @Test
    fun `previewConfigOverride captures only explicit values`() {
        val override = previewConfigOverride {
            thumbnailColumns = 4
            showTags = false
        }

        assertEquals(4, override.thumbnailColumns)
        assertEquals(false, override.showTags)
        assertNull(override.fontScale)
    }

    @Test
    fun `PreviewConfigOverride merges explicit values without consulting library defaults`() {
        val base = PreviewConfig(thumbnailColumns = 1, showTags = true)
        val override = previewConfigOverride { thumbnailColumns = 3 }

        val merged = base.overriddenBy(override)

        assertEquals(3, merged.thumbnailColumns)
        assertEquals(true, merged.showTags)
    }
}
