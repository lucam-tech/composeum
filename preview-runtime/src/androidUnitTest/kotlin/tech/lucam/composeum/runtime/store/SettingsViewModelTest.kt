package tech.lucam.composeum.runtime.store

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.compose.ui.graphics.Color
import tech.lucam.composeum.runtime.AccessibilityPreviewState
import tech.lucam.composeum.runtime.ColorBlindMode
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.ThemeOption
import tech.lucam.composeum.runtime.config.ThemeOptionDefaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsViewModelTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    // --- Merge logic tests (pure unit, no DataStore) ---

    @Test
    fun `all-null RuntimeSettings resolves to config defaults`() {
        val config = PreviewConfig(
            fontScale = 1.5f,
            uiScale = 0.8f,
            thumbnailColumns = 3,
            showDescriptions = false,
            showTags = false,
        )
        val resolved = RuntimeSettings().resolve(config)
        assertEquals(1.5f, resolved.fontScale)
        assertEquals(0.8f, resolved.uiScale)
        assertEquals(3, resolved.thumbnailColumns)
        assertFalse(resolved.showDescriptions)
        assertFalse(resolved.showTags)
        assertEquals(ThemeOptionDefaults.Classic, resolved.theme)
        assertEquals(AccessibilityPreviewState(), resolved.accessibilityState)
    }

    @Test
    fun `non-null runtime settings win over config defaults`() {
        val config = PreviewConfig(fontScale = 1.0f, uiScale = 1.0f, thumbnailColumns = 2)
        val resolved = RuntimeSettings(fontScale = 1.8f, uiScale = 0.5f, thumbnailColumns = 4).resolve(config)
        assertEquals(1.8f, resolved.fontScale)
        assertEquals(0.5f, resolved.uiScale)
        assertEquals(4, resolved.thumbnailColumns)
    }

    @Test
    fun `LIGHT theme override resolves isDark to false regardless of config`() {
        val config = PreviewConfig(isDarkMode = true)
        val resolved = RuntimeSettings(themeOverride = ThemeOverride.LIGHT).resolve(config)
        assertFalse(resolved.isDark)
    }

    @Test
    fun `DARK theme override resolves isDark to true regardless of config`() {
        val config = PreviewConfig(isDarkMode = false)
        val resolved = RuntimeSettings(themeOverride = ThemeOverride.DARK).resolve(config)
        assertTrue(resolved.isDark)
    }

    @Test
    fun `SYSTEM theme override uses config isDarkMode when set to true`() {
        val config = PreviewConfig(isDarkMode = true)
        val resolved = RuntimeSettings(themeOverride = ThemeOverride.SYSTEM).resolve(config)
        assertTrue(resolved.isDark)
    }

    @Test
    fun `SYSTEM theme override uses config isDarkMode when set to false`() {
        val config = PreviewConfig(isDarkMode = false)
        val resolved = RuntimeSettings(themeOverride = ThemeOverride.SYSTEM).resolve(config)
        assertFalse(resolved.isDark)
    }

    @Test
    fun `SYSTEM theme override defaults to false when config isDarkMode is null`() {
        val config = PreviewConfig(isDarkMode = null)
        val resolved = RuntimeSettings(themeOverride = ThemeOverride.SYSTEM).resolve(config)
        assertFalse(resolved.isDark)
    }

    @Test
    fun `runtime locale overrides config locale`() {
        val config = PreviewConfig(locale = "en")
        val resolved = RuntimeSettings(locale = "de").resolve(config)
        assertEquals("de", resolved.locale)
    }

    @Test
    fun `config locale used when runtime locale is null`() {
        val config = PreviewConfig(locale = "ja-JP")
        val resolved = RuntimeSettings().resolve(config)
        assertEquals("ja-JP", resolved.locale)
    }

    @Test
    fun `runtime accessibility modes override config defaults`() {
        val config = PreviewConfig(
            accessibilityState = AccessibilityPreviewState(),
        )
        val resolved = RuntimeSettings(
            screenReaderMode = true,
            highContrastMode = true,
            colorBlindMode = ColorBlindMode.DEUTERANOPIA,
            reducedMotionMode = true,
            largeTouchTargetsMode = true,
        ).resolve(config)

        assertTrue(resolved.accessibilityState.screenReaderMode)
        assertTrue(resolved.accessibilityState.highContrastMode)
        assertEquals(ColorBlindMode.DEUTERANOPIA, resolved.accessibilityState.colorBlindMode)
        assertTrue(resolved.accessibilityState.reducedMotionMode)
        assertTrue(resolved.accessibilityState.largeTouchTargetsMode)
    }

    @Test
    fun `config accessibility defaults are used when runtime values are null`() {
        val config = PreviewConfig(
            accessibilityState = AccessibilityPreviewState(
                screenReaderMode = true,
                highContrastMode = true,
                colorBlindMode = ColorBlindMode.PROTANOPIA,
                reducedMotionMode = true,
                largeTouchTargetsMode = true,
            ),
        )
        val resolved = RuntimeSettings().resolve(config)
        assertEquals(config.accessibilityState, resolved.accessibilityState)
    }

    @Test
    fun `runtime themeId overrides config default theme`() {
        val customTheme = ThemeOption("brand", "Brand", Color(0xFF102030), Color(0xFF405060))
        val config = PreviewConfig(
            defaultThemeId = ThemeOptionDefaults.Ocean.id,
            themeOptions = listOf(customTheme),
        )
        val resolved = RuntimeSettings(themeId = "brand").resolve(config)
        assertEquals(customTheme, resolved.theme)
    }

    @Test
    fun `config default theme is used when runtime themeId is null`() {
        val config = PreviewConfig(defaultThemeId = ThemeOptionDefaults.Forest.id)
        val resolved = RuntimeSettings().resolve(config)
        assertEquals(ThemeOptionDefaults.Forest, resolved.theme)
    }

    // --- StateFlow wiring test ---

    private fun TestScope.buildComponents(
        config: PreviewConfig = PreviewConfig(),
        fileName: String = "vm_settings.preferences_pb",
    ): Pair<DataStoreSettingsStorage, SettingsViewModel> {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tmpFolder.newFile(fileName) },
        )
        val storage = DataStoreSettingsStorage(store)
        val vm = SettingsViewModel(storage, config, backgroundScope)
        return storage to vm
    }

    @Test
    fun `resolvedSettings StateFlow reflects repository updates`() = runTest {
        val config = PreviewConfig(fontScale = 1.0f)
        val (repo, vm) = buildComponents(config)

        val emitted = mutableListOf<Float>()
        val collectJob = launch { vm.resolvedSettings.collect { emitted.add(it.fontScale) } }

        // Advance to let stateIn subscribe to the upstream flow and the collect job start.
        testScheduler.advanceUntilIdle()

        repo.update { copy(fontScale = 2.0f) }

        // Confirm DataStore persisted the value; this drains remaining IO/scheduler work.
        repo.settings.first { it.fontScale == 2.0f }
        testScheduler.advanceUntilIdle()

        assertTrue("Expected at least one emission", emitted.isNotEmpty())
        assertEquals(2.0f, emitted.last())
        collectJob.cancel()
    }

    @Test
    fun `resolvedSettings initial value uses config defaults`() = runTest {
        val config = PreviewConfig(fontScale = 1.3f, thumbnailColumns = 3)
        val (_, vm) = buildComponents(config)
        val initial = vm.resolvedSettings.value
        assertEquals(1.3f, initial.fontScale)
        assertEquals(3, initial.thumbnailColumns)
    }
}
