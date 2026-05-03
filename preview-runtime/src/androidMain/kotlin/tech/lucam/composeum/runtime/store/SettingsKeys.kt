package tech.lucam.composeum.runtime.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object SettingsKeys {
    val THEME_OVERRIDE = stringPreferencesKey("theme_override")
    val THEME_ID = stringPreferencesKey("theme_id")
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val UI_SCALE = floatPreferencesKey("ui_scale")
    val THUMBNAIL_COLUMNS = intPreferencesKey("thumbnail_columns")
    val SHOW_DESCRIPTIONS = booleanPreferencesKey("show_descriptions")
    val SHOW_TAGS = booleanPreferencesKey("show_tags")
    val LOCALE = stringPreferencesKey("locale")
    val LAST_ROUTE = stringPreferencesKey("last_route")
    val EXPANDED_GROUP_KEYS = stringPreferencesKey("expanded_group_keys")
    val INLINE_EXPANDED_GROUP_KEYS = stringPreferencesKey("inline_expanded_group_keys")
    val FAVORITES_EXPANDED = booleanPreferencesKey("favorites_expanded")
    val RECENT_EXPANDED = booleanPreferencesKey("recent_expanded")
    val FAVORITE_FAMILY_KEYS = stringPreferencesKey("favorite_family_keys")
    val RECENT_FAMILY_KEYS = stringPreferencesKey("recent_family_keys")
}
