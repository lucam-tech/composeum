package tech.lucam.composeum.runtime.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import tech.lucam.composeum.runtime.ColorBlindMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads and writes [RuntimeSettings] via [DataStore].
 *
 * Pass the [DataStore] from the composition root (created once in
 * [tech.lucam.composeum.runtime.ui.ComposeumBrowser] or the host activity).
 */
class DataStoreSettingsStorage(private val dataStore: DataStore<Preferences>) : SettingsStorage {

    override val settings: Flow<RuntimeSettings> = dataStore.data.map { it.toRuntimeSettings() }

    override suspend fun update(block: RuntimeSettings.() -> RuntimeSettings) {
        dataStore.edit { prefs ->
            prefs.writeSettings(prefs.toRuntimeSettings().block())
        }
    }

    override suspend fun reset() {
        dataStore.edit { prefs ->
            prefs -= SettingsKeys.THEME_OVERRIDE
            prefs -= SettingsKeys.THEME_ID
            prefs -= SettingsKeys.FONT_SCALE
            prefs -= SettingsKeys.UI_SCALE
            prefs -= SettingsKeys.THUMBNAIL_COLUMNS
            prefs -= SettingsKeys.SHOW_DESCRIPTIONS
            prefs -= SettingsKeys.SHOW_TAGS
            prefs -= SettingsKeys.LOCALE
            prefs -= SettingsKeys.SCREEN_READER_MODE
            prefs -= SettingsKeys.HIGH_CONTRAST_MODE
            prefs -= SettingsKeys.COLOR_BLIND_MODE
            prefs -= SettingsKeys.REDUCED_MOTION_MODE
            prefs -= SettingsKeys.LARGE_TOUCH_TARGETS_MODE
        }
    }

    private fun Preferences.toRuntimeSettings(): RuntimeSettings = RuntimeSettings(
        themeOverride = this[SettingsKeys.THEME_OVERRIDE]
            ?.let { runCatching { ThemeOverride.valueOf(it) }.getOrNull() }
            ?: ThemeOverride.SYSTEM,
        themeId = this[SettingsKeys.THEME_ID],
        fontScale = this[SettingsKeys.FONT_SCALE],
        uiScale = this[SettingsKeys.UI_SCALE],
        thumbnailColumns = this[SettingsKeys.THUMBNAIL_COLUMNS],
        showDescriptions = this[SettingsKeys.SHOW_DESCRIPTIONS],
        showTags = this[SettingsKeys.SHOW_TAGS],
        locale = this[SettingsKeys.LOCALE],
        screenReaderMode = this[SettingsKeys.SCREEN_READER_MODE],
        highContrastMode = this[SettingsKeys.HIGH_CONTRAST_MODE],
        colorBlindMode = this[SettingsKeys.COLOR_BLIND_MODE]
            ?.let { runCatching { ColorBlindMode.valueOf(it) }.getOrNull() },
        reducedMotionMode = this[SettingsKeys.REDUCED_MOTION_MODE],
        largeTouchTargetsMode = this[SettingsKeys.LARGE_TOUCH_TARGETS_MODE],
    )

    private fun MutablePreferences.writeSettings(s: RuntimeSettings) {
        this[SettingsKeys.THEME_OVERRIDE] = s.themeOverride.name
        if (s.themeId != null) this[SettingsKeys.THEME_ID] = s.themeId
        else this -= SettingsKeys.THEME_ID
        if (s.fontScale != null) this[SettingsKeys.FONT_SCALE] = s.fontScale
        else this -= SettingsKeys.FONT_SCALE
        if (s.uiScale != null) this[SettingsKeys.UI_SCALE] = s.uiScale
        else this -= SettingsKeys.UI_SCALE
        if (s.thumbnailColumns != null) this[SettingsKeys.THUMBNAIL_COLUMNS] = s.thumbnailColumns
        else this -= SettingsKeys.THUMBNAIL_COLUMNS
        if (s.showDescriptions != null) this[SettingsKeys.SHOW_DESCRIPTIONS] = s.showDescriptions
        else this -= SettingsKeys.SHOW_DESCRIPTIONS
        if (s.showTags != null) this[SettingsKeys.SHOW_TAGS] = s.showTags
        else this -= SettingsKeys.SHOW_TAGS
        if (s.locale != null) this[SettingsKeys.LOCALE] = s.locale
        else this -= SettingsKeys.LOCALE
        if (s.screenReaderMode != null) this[SettingsKeys.SCREEN_READER_MODE] = s.screenReaderMode
        else this -= SettingsKeys.SCREEN_READER_MODE
        if (s.highContrastMode != null) this[SettingsKeys.HIGH_CONTRAST_MODE] = s.highContrastMode
        else this -= SettingsKeys.HIGH_CONTRAST_MODE
        if (s.colorBlindMode != null) this[SettingsKeys.COLOR_BLIND_MODE] = s.colorBlindMode.name
        else this -= SettingsKeys.COLOR_BLIND_MODE
        if (s.reducedMotionMode != null) this[SettingsKeys.REDUCED_MOTION_MODE] = s.reducedMotionMode
        else this -= SettingsKeys.REDUCED_MOTION_MODE
        if (s.largeTouchTargetsMode != null) this[SettingsKeys.LARGE_TOUCH_TARGETS_MODE] = s.largeTouchTargetsMode
        else this -= SettingsKeys.LARGE_TOUCH_TARGETS_MODE
    }
}
