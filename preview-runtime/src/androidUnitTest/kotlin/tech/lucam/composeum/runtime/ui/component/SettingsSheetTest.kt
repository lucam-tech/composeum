package tech.lucam.composeum.runtime.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import tech.lucam.composeum.runtime.config.BuiltInSettingId
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.SettingItem
import tech.lucam.composeum.runtime.config.ThemeOption
import tech.lucam.composeum.runtime.config.ThemeOptionDefaults
import tech.lucam.composeum.runtime.store.RuntimeSettings
import tech.lucam.composeum.runtime.store.SettingsStorage
import tech.lucam.composeum.runtime.store.ThemeOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsSheetTest {

    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    private lateinit var repo: FakeSettingsStorage

    private val defaultConfig = PreviewConfig()

    @Before
    fun setUp() {
        repo = FakeSettingsStorage()
    }

    @After
    fun tearDown() {
    }

    private fun setContent(config: PreviewConfig = defaultConfig) {
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalSettingsStorage provides repo) {
                    SettingsSheet(config = config)
                }
            }
        }
    }

    private fun awaitSettings(condition: (RuntimeSettings) -> Boolean) =
        runBlocking { repo.settings.first(condition) }

    private class FakeSettingsStorage(
        initial: RuntimeSettings = RuntimeSettings(),
    ) : SettingsStorage {
        private val state = MutableStateFlow(initial)

        override val settings: Flow<RuntimeSettings> = state

        override suspend fun update(block: RuntimeSettings.() -> RuntimeSettings) {
            state.value = state.value.block()
        }

        override suspend fun reset() {
            state.value = RuntimeSettings()
        }
    }

    private fun configWithOnly(settingId: String): PreviewConfig = defaultConfig.copy(
        settingsItems = listOf(SettingItem.BuiltIn(settingId)),
    )

    // ── Theme toggle ──────────────────────────────────────────────────────────

    @Test
    fun `theme Light segment updates themeOverride to LIGHT`() {
        setContent(configWithOnly(BuiltInSettingId.THEME))
        composeRule.onNodeWithText("Light").performClick()
        val settings = awaitSettings { it.themeOverride == ThemeOverride.LIGHT }
        assertEquals(ThemeOverride.LIGHT, settings.themeOverride)
    }

    @Test
    fun `theme Dark segment updates themeOverride to DARK`() {
        setContent(configWithOnly(BuiltInSettingId.THEME))
        composeRule.onNodeWithText("Dark").performClick()
        val settings = awaitSettings { it.themeOverride == ThemeOverride.DARK }
        assertEquals(ThemeOverride.DARK, settings.themeOverride)
    }

    @Test
    fun `theme System segment updates themeOverride to SYSTEM`() {
        // First set to DARK so we can verify SYSTEM is written
        runBlocking { repo.update { copy(themeOverride = ThemeOverride.DARK) } }
        setContent(configWithOnly(BuiltInSettingId.THEME))
        composeRule.onNodeWithText("System").performClick()
        val settings = awaitSettings { it.themeOverride == ThemeOverride.SYSTEM }
        assertEquals(ThemeOverride.SYSTEM, settings.themeOverride)
    }

    @Test
    fun `selecting built in theme updates themeId`() {
        setContent(configWithOnly(BuiltInSettingId.THEME))
        composeRule.onNodeWithContentDescription("Theme dropdown").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Ocean"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Ocean", useUnmergedTree = true).performClick()
        val settings = awaitSettings { it.themeId == ThemeOptionDefaults.Ocean.id }
        assertEquals(ThemeOptionDefaults.Ocean.id, settings.themeId)
    }

    @Test
    fun `custom theme from config is shown in theme dropdown`() {
        val customTheme = ThemeOption("brand", "Brand", Color(0xFF123456), Color(0xFF654321))
        setContent(
            configWithOnly(BuiltInSettingId.THEME).copy(themeOptions = listOf(customTheme)),
        )
        composeRule.onNodeWithContentDescription("Theme dropdown").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Brand"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Brand", useUnmergedTree = true).assertIsDisplayed()
    }

    // ── Font scale slider ─────────────────────────────────────────────────────

    @Test
    fun `font scale slider is displayed`() {
        setContent(configWithOnly(BuiltInSettingId.FONT_SCALE))
        composeRule.onNodeWithContentDescription("Font scale slider").assertIsDisplayed()
    }

    @Test
    fun `font scale slider SetProgress updates fontScale in repository`() {
        setContent(configWithOnly(BuiltInSettingId.FONT_SCALE))
        composeRule
            .onNode(hasContentDescription("Font scale slider"))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.5f) }
        val settings = awaitSettings { it.fontScale != null }
        assertEquals(1.5f, settings.fontScale!!, 0.05f)
    }

    // ── UI scale slider ───────────────────────────────────────────────────────

    @Test
    fun `ui scale slider is displayed`() {
        setContent(configWithOnly(BuiltInSettingId.UI_SCALE))
        composeRule.onNodeWithContentDescription("UI scale slider").assertIsDisplayed()
    }

    @Test
    fun `ui scale slider SetProgress updates uiScale in repository`() {
        setContent(configWithOnly(BuiltInSettingId.UI_SCALE))
        composeRule
            .onNode(hasContentDescription("UI scale slider"))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }
        val settings = awaitSettings { it.uiScale != null }
        assertEquals(0.75f, settings.uiScale!!, 0.05f)
    }

    // ── Thumbnail columns stepper ─────────────────────────────────────────────

    @Test
    fun `increase thumbnail columns button increments columns`() {
        setContent(configWithOnly(BuiltInSettingId.THUMBNAIL_COLUMNS))
        composeRule.onNodeWithContentDescription("Increase thumbnail columns").performClick()
        val settings = awaitSettings { it.thumbnailColumns != null }
        assertEquals(3, settings.thumbnailColumns) // default resolved = 2, +1 = 3
    }

    @Test
    fun `decrease thumbnail columns button decrements columns`() {
        runBlocking { repo.update { copy(thumbnailColumns = 3) } }
        setContent(configWithOnly(BuiltInSettingId.THUMBNAIL_COLUMNS))
        composeRule.onNodeWithContentDescription("Decrease thumbnail columns").performClick()
        val settings = awaitSettings { it.thumbnailColumns == 2 }
        assertEquals(2, settings.thumbnailColumns)
    }

    @Test
    fun `decrease button is disabled at minimum columns`() {
        runBlocking { repo.update { copy(thumbnailColumns = 1) } }
        setContent(configWithOnly(BuiltInSettingId.THUMBNAIL_COLUMNS))
        // With columns at 1, the decrease button should be disabled — clicking does nothing
        val before = runBlocking { repo.settings.first() }
        composeRule.onNodeWithContentDescription("Decrease thumbnail columns").performClick()
        val after = runBlocking { repo.settings.first() }
        assertEquals(before.thumbnailColumns, after.thumbnailColumns)
    }

    @Test
    fun `increase button is disabled at maximum columns`() {
        runBlocking { repo.update { copy(thumbnailColumns = 4) } }
        setContent(configWithOnly(BuiltInSettingId.THUMBNAIL_COLUMNS))
        val before = runBlocking { repo.settings.first() }
        composeRule.onNodeWithContentDescription("Increase thumbnail columns").performClick()
        val after = runBlocking { repo.settings.first() }
        assertEquals(before.thumbnailColumns, after.thumbnailColumns)
    }

    // ── Show descriptions ─────────────────────────────────────────────────────

    @Test
    fun `show descriptions toggle updates showDescriptions`() {
        setContent(configWithOnly(BuiltInSettingId.SHOW_DESCRIPTIONS))
        composeRule.onNodeWithContentDescription("Show descriptions toggle").performClick()
        val settings = awaitSettings { it.showDescriptions != null }
        assertEquals(false, settings.showDescriptions) // default resolved = true → toggled to false
    }

    // ── Show tags ─────────────────────────────────────────────────────────────

    @Test
    fun `show tags toggle updates showTags`() {
        setContent(configWithOnly(BuiltInSettingId.SHOW_TAGS))
        composeRule.onNodeWithContentDescription("Show tags toggle").performClick()
        val settings = awaitSettings { it.showTags != null }
        assertEquals(false, settings.showTags) // default resolved = true → toggled to false
    }

    // ── Locale dropdown ───────────────────────────────────────────────────────

    @Test
    fun `selecting Deutsch from locale dropdown updates locale to de`() {
        setContent(configWithOnly(BuiltInSettingId.LOCALE))
        composeRule.onNodeWithContentDescription("Locale dropdown").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Deutsch"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Deutsch", useUnmergedTree = true).performClick()
        val settings = awaitSettings { it.locale == "de" }
        assertEquals("de", settings.locale)
    }

    @Test
    fun `selecting System from locale dropdown clears locale key`() {
        runBlocking { repo.update { copy(locale = "fr") } }
        setContent(configWithOnly(BuiltInSettingId.LOCALE))
        composeRule.onNodeWithContentDescription("Locale dropdown").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("System"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("System", useUnmergedTree = true).performClick()
        val settings = awaitSettings { it.locale == null }
        assertNull(settings.locale)
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    fun `reset button clears all persisted settings`() {
        runBlocking {
            repo.update {
                copy(
                    themeOverride = ThemeOverride.DARK,
                    themeId = ThemeOptionDefaults.Sunset.id,
                    fontScale = 1.8f,
                    uiScale = 1.5f,
                    thumbnailColumns = 4,
                    showDescriptions = false,
                    showTags = false,
                    locale = "ja",
                )
            }
        }
        setContent(configWithOnly(BuiltInSettingId.RESET))
        composeRule.onNodeWithText("Reset all settings").performClick()
        val settings = awaitSettings { it.fontScale == null && it.locale == null }
        assertEquals(ThemeOverride.SYSTEM, settings.themeOverride)
        assertNull(settings.themeId)
        assertNull(settings.fontScale)
        assertNull(settings.uiScale)
        assertNull(settings.thumbnailColumns)
        assertNull(settings.showDescriptions)
        assertNull(settings.showTags)
        assertNull(settings.locale)
    }
}
