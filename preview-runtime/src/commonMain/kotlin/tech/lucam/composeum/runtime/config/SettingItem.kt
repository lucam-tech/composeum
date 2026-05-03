package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Composable

/**
 * An item in the settings bottom sheet.
 *
 * Pass a list of [SettingItem]s to [PreviewConfig.settingsItems] to control which settings
 * appear and in what order. Omitting a [BuiltIn] ID removes that setting. When
 * [PreviewConfig.settingsItems] is `null` all built-ins are shown in their default order.
 *
 * @see BuiltInSettingId
 */
sealed class SettingItem {
    abstract val id: String

    /** Includes a built-in setting control. See [BuiltInSettingId] for valid IDs. */
    data class BuiltIn(override val id: String) : SettingItem()

    /** Injects an arbitrary composable into the settings sheet at this position. */
    class Custom(override val id: String, val content: @Composable () -> Unit) : SettingItem()
}

/** Stable IDs for every built-in settings control, for use with [SettingItem.BuiltIn]. */
object BuiltInSettingId {
    const val THEME = "theme"
    const val FONT_SCALE = "font_scale"
    const val UI_SCALE = "ui_scale"
    const val THUMBNAIL_COLUMNS = "thumbnail_columns"
    const val SHOW_DESCRIPTIONS = "show_descriptions"
    const val SHOW_TAGS = "show_tags"
    const val LOCALE = "locale"
    const val RESET = "reset"
}
