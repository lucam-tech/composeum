package tech.lucam.composeum.runtime.store

import kotlinx.serialization.Serializable
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.ThemeOptionDefaults
import tech.lucam.composeum.runtime.config.resolvedThemeOptions

/** User-selectable theme override stored in DataStore. */
@Serializable
enum class ThemeOverride { LIGHT, DARK, SYSTEM }

/**
 * Mutable overlay that the settings bottom sheet writes to persistent storage.
 * Null fields mean "use the [PreviewConfig] default".
 */
@Serializable
data class RuntimeSettings(
    /** Active theme override; defaults to [ThemeOverride.SYSTEM]. */
    val themeOverride: ThemeOverride = ThemeOverride.SYSTEM,
    /** Selected theme id, or null to fall back to [PreviewConfig.defaultThemeId]. */
    val themeId: String? = null,
    /** User-set font scale, or null to fall back to [PreviewConfig.fontScale]. */
    val fontScale: Float? = null,
    /** User-set UI (density) scale, or null to fall back to [PreviewConfig.uiScale]. */
    val uiScale: Float? = null,
    /** User-set thumbnail column count, or null to fall back to [PreviewConfig.thumbnailColumns]. */
    val thumbnailColumns: Int? = null,
    /** User-set description visibility, or null to fall back to [PreviewConfig.showDescriptions]. */
    val showDescriptions: Boolean? = null,
    /** User-set tag visibility, or null to fall back to [PreviewConfig.showTags]. */
    val showTags: Boolean? = null,
    /** BCP 47 locale tag (e.g. "en-US"), or null to use the device default. */
    val locale: String? = null,
    /** Last browser route encoded by [tech.lucam.composeum.runtime.ui.PreviewRoute]. */
    val lastRoute: String? = null,
    /** Persisted expanded non-leaf group keys. Null means use runtime defaults. */
    val expandedGroupKeys: List<String>? = null,
    /** Persisted inline-expanded leaf group keys. Null means use runtime defaults. */
    val inlineExpandedGroupKeys: List<String>? = null,
    /** Whether the Favorites section is expanded on the root screen. Null means expanded. */
    val favoritesExpanded: Boolean? = null,
    /** Whether the Recent section is expanded on the root screen. Null means expanded. */
    val recentExpanded: Boolean? = null,
    /** Favorited preview family keys. */
    val favoriteFamilyKeys: List<String> = emptyList(),
    /** Most-recently opened preview family keys, newest first. */
    val recentFamilyKeys: List<String> = emptyList(),
)

/**
 * The fully-resolved settings exposed to the UI, produced by merging
 * [PreviewConfig] and [RuntimeSettings]. [RuntimeSettings] wins on every
 * non-null field.
 *
 * [locale] is a BCP 47 locale tag string (e.g. "en-US"). Platform-specific
 * code converts it to a native Locale when applying it to the composition.
 */
data class ResolvedSettings(
    /** Whether dark mode is active. */
    val isDark: Boolean,
    /** Active browser theme colors. */
    val theme: tech.lucam.composeum.runtime.config.ThemeOption,
    /** Active font scale multiplier. */
    val fontScale: Float,
    /** Active UI (density) scale multiplier. */
    val uiScale: Float,
    /** Active number of thumbnail grid columns. */
    val thumbnailColumns: Int,
    /** Whether group/preview descriptions are visible. */
    val showDescriptions: Boolean,
    /** Whether tag chips are visible on thumbnail cards. */
    val showTags: Boolean,
    /** BCP 47 locale tag, or `"system"` to use the device default. */
    val locale: String,
) {
    companion object {
        /** Neutral defaults used as the [LocalResolvedSettings] fallback value. */
        val DEFAULT = ResolvedSettings(
            isDark = false,
            theme = ThemeOptionDefaults.Classic,
            fontScale = 1f,
            uiScale = 1f,
            thumbnailColumns = 2,
            showDescriptions = true,
            showTags = true,
            locale = "system",
        )
    }
}

/**
 * Resolves [RuntimeSettings] against [config] into the final [ResolvedSettings].
 *
 * **Note**: [ResolvedSettings.isDark] is only accurate for [ThemeOverride.LIGHT] and
 * [ThemeOverride.DARK]. When [themeOverride] is [ThemeOverride.SYSTEM], the caller must
 * read `isSystemInDarkTheme()` from the Compose context and apply it itself — there is
 * no way to access composable state from a pure function. [ComposeumBrowser] handles this.
 *
 * @param systemIsDark The actual device dark-mode state, read via `isSystemInDarkTheme()`.
 *                     Only used when [themeOverride] is [ThemeOverride.SYSTEM].
 */
internal fun RuntimeSettings.resolve(
    config: PreviewConfig,
    systemIsDark: Boolean = false
): ResolvedSettings {
    val themeOptions = config.resolvedThemeOptions()
    val selectedThemeId = themeId ?: config.defaultThemeId
    val selectedTheme =
        themeOptions.firstOrNull { it.id == selectedThemeId } ?: themeOptions.first()
    val isDark = when (themeOverride) {
        ThemeOverride.LIGHT -> false
        ThemeOverride.DARK -> true
        ThemeOverride.SYSTEM -> config.isDarkMode ?: systemIsDark
    }
    return ResolvedSettings(
        isDark = isDark,
        theme = selectedTheme,
        fontScale = fontScale ?: config.fontScale,
        uiScale = uiScale ?: config.uiScale,
        thumbnailColumns = thumbnailColumns ?: config.thumbnailColumns,
        showDescriptions = showDescriptions ?: config.showDescriptions,
        showTags = showTags ?: config.showTags,
        locale = locale ?: config.locale ?: "system",
    )
}
