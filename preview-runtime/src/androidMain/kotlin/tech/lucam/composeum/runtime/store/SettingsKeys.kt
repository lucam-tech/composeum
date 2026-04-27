package tech.lucam.composeum.runtime.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object SettingsKeys {
    val THEME_OVERRIDE = stringPreferencesKey("theme_override")
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val UI_SCALE = floatPreferencesKey("ui_scale")
    val THUMBNAIL_COLUMNS = intPreferencesKey("thumbnail_columns")
    val SHOW_DESCRIPTIONS = booleanPreferencesKey("show_descriptions")
    val SHOW_TAGS = booleanPreferencesKey("show_tags")
    val LOCALE = stringPreferencesKey("locale")
}
