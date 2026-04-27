package tech.lucam.composeum.runtime.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.store.DataStoreSettingsStorage
import tech.lucam.composeum.runtime.store.ThemeOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsSheetTest {

    @get:Rule(order = 0)
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private lateinit var repo: DataStoreSettingsStorage
    private lateinit var testScope: CoroutineScope

    private val defaultConfig = PreviewConfig()

    @Before
    fun setUp() {
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("settings.preferences_pb") },
        )
        repo = DataStoreSettingsStorage(store)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    private fun setContent() {
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalSettingsStorage provides repo) {
                    SettingsSheet(config = defaultConfig)
                }
            }
        }
    }

    private fun awaitSettings(condition: (tech.lucam.composeum.runtime.store.RuntimeSettings) -> Boolean) =
        runBlocking { repo.settings.first(condition) }

    // ── Theme toggle ──────────────────────────────────────────────────────────

    @Test
    fun `theme Light segment updates themeOverride to LIGHT`() {
        setContent()
        composeRule.onNodeWithText("Light").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.themeOverride == ThemeOverride.LIGHT }
        assertEquals(ThemeOverride.LIGHT, settings.themeOverride)
    }

    @Test
    fun `theme Dark segment updates themeOverride to DARK`() {
        setContent()
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.themeOverride == ThemeOverride.DARK }
        assertEquals(ThemeOverride.DARK, settings.themeOverride)
    }

    @Test
    fun `theme System segment updates themeOverride to SYSTEM`() {
        // First set to DARK so we can verify SYSTEM is written
        runBlocking { repo.update { copy(themeOverride = ThemeOverride.DARK) } }
        setContent()
        composeRule.onNodeWithText("System").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.themeOverride == ThemeOverride.SYSTEM }
        assertEquals(ThemeOverride.SYSTEM, settings.themeOverride)
    }

    // ── Font scale slider ─────────────────────────────────────────────────────

    @Test
    fun `font scale slider is displayed`() {
        setContent()
        composeRule.onNodeWithContentDescription("Font scale slider").assertIsDisplayed()
    }

    @Test
    fun `font scale slider SetProgress updates fontScale in repository`() {
        setContent()
        composeRule
            .onNode(hasContentDescription("Font scale slider"))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.5f) }
        composeRule.waitForIdle()
        val settings = awaitSettings { it.fontScale != null }
        assertEquals(1.5f, settings.fontScale!!, 0.05f)
    }

    // ── UI scale slider ───────────────────────────────────────────────────────

    @Test
    fun `ui scale slider is displayed`() {
        setContent()
        composeRule.onNodeWithContentDescription("UI scale slider").assertIsDisplayed()
    }

    @Test
    fun `ui scale slider SetProgress updates uiScale in repository`() {
        setContent()
        composeRule
            .onNode(hasContentDescription("UI scale slider"))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }
        composeRule.waitForIdle()
        val settings = awaitSettings { it.uiScale != null }
        assertEquals(0.75f, settings.uiScale!!, 0.05f)
    }

    // ── Thumbnail columns stepper ─────────────────────────────────────────────

    @Test
    fun `increase thumbnail columns button increments columns`() {
        setContent()
        composeRule.onNodeWithContentDescription("Increase thumbnail columns").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.thumbnailColumns != null }
        assertEquals(3, settings.thumbnailColumns) // default resolved = 2, +1 = 3
    }

    @Test
    fun `decrease thumbnail columns button decrements columns`() {
        runBlocking { repo.update { copy(thumbnailColumns = 3) } }
        setContent()
        composeRule.onNodeWithContentDescription("Decrease thumbnail columns").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.thumbnailColumns == 2 }
        assertEquals(2, settings.thumbnailColumns)
    }

    @Test
    fun `decrease button is disabled at minimum columns`() {
        runBlocking { repo.update { copy(thumbnailColumns = 1) } }
        setContent()
        // With columns at 1, the decrease button should be disabled — clicking does nothing
        val before = runBlocking { repo.settings.first() }
        composeRule.onNodeWithContentDescription("Decrease thumbnail columns").performClick()
        composeRule.waitForIdle()
        val after = runBlocking { repo.settings.first() }
        assertEquals(before.thumbnailColumns, after.thumbnailColumns)
    }

    @Test
    fun `increase button is disabled at maximum columns`() {
        runBlocking { repo.update { copy(thumbnailColumns = 4) } }
        setContent()
        val before = runBlocking { repo.settings.first() }
        composeRule.onNodeWithContentDescription("Increase thumbnail columns").performClick()
        composeRule.waitForIdle()
        val after = runBlocking { repo.settings.first() }
        assertEquals(before.thumbnailColumns, after.thumbnailColumns)
    }

    // ── Show descriptions ─────────────────────────────────────────────────────

    @Test
    fun `show descriptions toggle updates showDescriptions`() {
        setContent()
        composeRule.onNodeWithContentDescription("Show descriptions toggle").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.showDescriptions != null }
        assertEquals(false, settings.showDescriptions) // default resolved = true → toggled to false
    }

    // ── Show tags ─────────────────────────────────────────────────────────────

    @Test
    fun `show tags toggle updates showTags`() {
        setContent()
        composeRule.onNodeWithContentDescription("Show tags toggle").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.showTags != null }
        assertEquals(false, settings.showTags) // default resolved = true → toggled to false
    }

    // ── Locale dropdown ───────────────────────────────────────────────────────

    @Test
    fun `selecting Deutsch from locale dropdown updates locale to de`() {
        setContent()
        // Open the dropdown
        composeRule.onNodeWithContentDescription("Locale dropdown").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Deutsch").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.locale == "de" }
        assertEquals("de", settings.locale)
    }

    @Test
    fun `selecting System from locale dropdown clears locale key`() {
        runBlocking { repo.update { copy(locale = "fr") } }
        setContent()
        composeRule.onNodeWithText("Français").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("System").performClick()
        composeRule.waitForIdle()
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
                    fontScale = 1.8f,
                    uiScale = 1.5f,
                    thumbnailColumns = 4,
                    showDescriptions = false,
                    showTags = false,
                    locale = "ja",
                )
            }
        }
        setContent()
        composeRule.onNodeWithText("Reset all settings").performClick()
        composeRule.waitForIdle()
        val settings = awaitSettings { it.fontScale == null && it.locale == null }
        assertEquals(ThemeOverride.SYSTEM, settings.themeOverride)
        assertNull(settings.fontScale)
        assertNull(settings.uiScale)
        assertNull(settings.thumbnailColumns)
        assertNull(settings.showDescriptions)
        assertNull(settings.showTags)
        assertNull(settings.locale)
    }
}
