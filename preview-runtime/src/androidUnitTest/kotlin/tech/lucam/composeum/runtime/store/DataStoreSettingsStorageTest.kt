package tech.lucam.composeum.runtime.store

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsStorageTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun TestScope.buildStorage(fileName: String = "test_settings.preferences_pb"): DataStoreSettingsStorage {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tmpFolder.newFile(fileName) },
        )
        return DataStoreSettingsStorage(store)
    }

    @Test
    fun `settings emits default RuntimeSettings when DataStore is empty`() = runTest {
        val storage = buildStorage()
        val settings = storage.settings.first()
        assertEquals(ThemeOverride.SYSTEM, settings.themeOverride)
        assertNull(settings.themeId)
        assertNull(settings.fontScale)
        assertNull(settings.uiScale)
        assertNull(settings.thumbnailColumns)
        assertNull(settings.showDescriptions)
        assertNull(settings.showTags)
        assertNull(settings.locale)
    }

    @Test
    fun `update persists themeOverride`() = runTest {
        val storage = buildStorage()
        storage.update { copy(themeOverride = ThemeOverride.DARK) }
        assertEquals(ThemeOverride.DARK, storage.settings.first().themeOverride)
    }

    @Test
    fun `update persists themeId`() = runTest {
        val storage = buildStorage()
        storage.update { copy(themeId = "ocean") }
        assertEquals("ocean", storage.settings.first().themeId)
    }

    @Test
    fun `update persists fontScale`() = runTest {
        val storage = buildStorage()
        storage.update { copy(fontScale = 1.5f) }
        assertEquals(1.5f, storage.settings.first().fontScale)
    }

    @Test
    fun `update persists uiScale`() = runTest {
        val storage = buildStorage()
        storage.update { copy(uiScale = 0.75f) }
        assertEquals(0.75f, storage.settings.first().uiScale)
    }

    @Test
    fun `update persists thumbnailColumns`() = runTest {
        val storage = buildStorage()
        storage.update { copy(thumbnailColumns = 3) }
        assertEquals(3, storage.settings.first().thumbnailColumns)
    }

    @Test
    fun `update persists showDescriptions`() = runTest {
        val storage = buildStorage()
        storage.update { copy(showDescriptions = false) }
        assertEquals(false, storage.settings.first().showDescriptions)
    }

    @Test
    fun `update persists showTags`() = runTest {
        val storage = buildStorage()
        storage.update { copy(showTags = false) }
        assertEquals(false, storage.settings.first().showTags)
    }

    @Test
    fun `update persists locale`() = runTest {
        val storage = buildStorage()
        storage.update { copy(locale = "de") }
        assertEquals("de", storage.settings.first().locale)
    }

    @Test
    fun `reset clears all keys so settings reverts to defaults`() = runTest {
        val storage = buildStorage()
        storage.update {
            copy(
                themeOverride = ThemeOverride.LIGHT,
                themeId = "sunset",
                fontScale = 2.0f,
                uiScale = 2.0f,
                thumbnailColumns = 4,
                showDescriptions = false,
                showTags = false,
                locale = "fr",
            )
        }
        storage.reset()
        val settings = storage.settings.first()
        assertEquals(ThemeOverride.SYSTEM, settings.themeOverride)
        assertNull(settings.themeId)
        assertNull(settings.fontScale)
        assertNull(settings.uiScale)
        assertNull(settings.thumbnailColumns)
        assertNull(settings.showDescriptions)
        assertNull(settings.showTags)
        assertNull(settings.locale)
    }

    @Test
    fun `sequential updates accumulate correctly`() = runTest {
        val storage = buildStorage()
        storage.update { copy(fontScale = 1.2f) }
        storage.update { copy(uiScale = 0.8f) }
        val settings = storage.settings.first()
        assertEquals(1.2f, settings.fontScale)
        assertEquals(0.8f, settings.uiScale)
    }
}
