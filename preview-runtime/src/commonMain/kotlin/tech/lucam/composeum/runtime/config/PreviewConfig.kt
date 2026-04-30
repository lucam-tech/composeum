package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Immutable
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.AccessibilityPreviewState
import kotlin.reflect.KClass

/** Immutable configuration for the preview browser. Build with [previewConfig]. */
@Immutable
data class PreviewConfig(
    /** Initial font scale applied before any user override from the settings sheet. */
    val fontScale: Float = 1.0f,
    /** Initial UI (density) scale applied before any user override from the settings sheet. */
    val uiScale: Float = 1.0f,
    /**
     * Overrides the system dark-mode when non-null. `null` (default) honours the system setting.
     * Only consulted when [PreviewConfig.browserWrapper] is null; custom wrappers receive the
     * resolved value via [tech.lucam.composeum.runtime.ui.LocalIsDarkTheme] and the selected
     * palette via [tech.lucam.composeum.runtime.ui.LocalPreviewTheme].
     */
    val isDarkMode: Boolean? = null,
    /** Theme id selected before any user override from the settings sheet. */
    val defaultThemeId: String = ThemeOptionDefaults.Classic.id,
    /** BCP 47 locale tag (e.g. "en-US", "fr"), or null to inherit the device locale. */
    val locale: String? = null,
    /** Default accessibility test-mode state applied before any persisted user override. */
    val accessibilityState: AccessibilityPreviewState = AccessibilityPreviewState(),
    /** Whether group descriptions are shown in the group list by default. */
    val showDescriptions: Boolean = true,
    /** Whether tag chips are shown on thumbnail cards by default. */
    val showTags: Boolean = true,
    /** Whether the param panel is visible on the detail screen. */
    val showParamPanel: Boolean = true,
    /** Number of thumbnail columns in the preview grid by default. */
    val thumbnailColumns: Int = 2,
    /** How leaf groups behave when tapped — navigate to a subscreen or expand inline. */
    val groupExpansionMode: GroupExpansionMode = GroupExpansionMode.SUBSCREEN,
    /**
     * Settings controls to display in the settings sheet, in order.
     * `null` (default) shows all built-ins. Supply a non-null list to choose which settings
     * appear — omit a [BuiltInSettingId] to remove it, or add [SettingItem.Custom] entries
     * to inject your own composables.
     */
    val settingsItems: List<SettingItem>? = null,
    /** Extra icon buttons prepended to the top app bar before the settings icon. */
    val topBarActions: List<TopBarAction> = emptyList(),
    /**
     * Base URL of your source repository used to construct links from the preview detail screen.
     * Example: `"https://gitlab.com/org/repo/-/blob/main"`.
     * When null and source location is available, the button opens an `idea://open` deep link
     * instead (works when Android Studio is connected via ADB).
     */
    val sourceBaseUrl: String? = null,
    /**
     * Path prefix to strip from [tech.lucam.composeum.runtime.PreviewEntry.sourceFile] before
     * appending to [sourceBaseUrl]. Typically the absolute path to the project root on the build
     * machine. Example: `"/home/user/workspace/my-project"`.
     */
    val sourceStripPrefix: String? = null,
    val browserWrapper: BrowserWrapper? = null,
    val groupWrapper: GroupWrapper? = null,
    val previewWrapper: PreviewWrapper? = null,
    val accessibilityWrapper: AccessibilityWrapper? = null,
    val groupOverrides: Map<KClass<out PreviewGroup>, GroupConfig> = emptyMap(),
    val previewOverrides: Map<String, PreviewOverride> = emptyMap(),
    /**
     * Locale options shown in the settings sheet locale picker.
     * Each [LocaleOption] has a BCP 47 [LocaleOption.tag] and a human-readable [LocaleOption.displayName].
     * Use `"system"` as the tag to represent the device default.
     * Pass `null` (default) to use the built-in list (System + en, de, fr, es, ja, ar).
     */
    val localeOptions: List<LocaleOption>? = null,
    /**
     * Additional themes shown in the settings sheet.
     * Built-ins from [ThemeOptionDefaults] are always registered first; duplicate ids replace them.
     */
    val themeOptions: List<ThemeOption> = emptyList(),
    /**
     * Custom param-panel widgets for types the library does not natively support.
     *
     * The map key is the **fully-qualified Kotlin type name** of the parameter
     * (e.g. `"com.example.StarRating"`). Use [customParamField] to build values
     * with full type safety:
     *
     * ```kotlin
     * customTypeFields = mapOf(
     *     "com.example.StarRating" to customParamField<StarRating>(
     *         initialValue = StarRating(3),
     *     ) { value, onValue ->
     *         StarRatingWidget(value, onValue)
     *     }
     * )
     * ```
     *
     * When a registered type is encountered in the param panel, the custom widget
     * is rendered instead of the default string text field. The initial value is
     * seeded into state before the first render so the composable always receives
     * a properly typed value.
     */
    val customTypeFields: Map<String, CustomParamField> = emptyMap(),
)

internal fun PreviewConfig.mergedWith(override: PreviewConfig): PreviewConfig {
    val defaults = PreviewConfig()
    return PreviewConfig(
        fontScale = if (override.fontScale != defaults.fontScale) override.fontScale else fontScale,
        uiScale = if (override.uiScale != defaults.uiScale) override.uiScale else uiScale,
        isDarkMode = if (override.isDarkMode != defaults.isDarkMode) override.isDarkMode else isDarkMode,
        defaultThemeId = if (override.defaultThemeId != defaults.defaultThemeId) override.defaultThemeId else defaultThemeId,
        locale = if (override.locale != defaults.locale) override.locale else locale,
        accessibilityState = if (override.accessibilityState != defaults.accessibilityState) {
            override.accessibilityState
        } else {
            accessibilityState
        },
        showDescriptions = if (override.showDescriptions != defaults.showDescriptions) override.showDescriptions else showDescriptions,
        showTags = if (override.showTags != defaults.showTags) override.showTags else showTags,
        showParamPanel = if (override.showParamPanel != defaults.showParamPanel) override.showParamPanel else showParamPanel,
        thumbnailColumns = if (override.thumbnailColumns != defaults.thumbnailColumns) override.thumbnailColumns else thumbnailColumns,
        groupExpansionMode = if (override.groupExpansionMode != defaults.groupExpansionMode) override.groupExpansionMode else groupExpansionMode,
        settingsItems = override.settingsItems ?: settingsItems,
        topBarActions = topBarActions + override.topBarActions,
        sourceBaseUrl = override.sourceBaseUrl ?: sourceBaseUrl,
        sourceStripPrefix = override.sourceStripPrefix ?: sourceStripPrefix,
        browserWrapper = override.browserWrapper ?: browserWrapper,
        groupWrapper = override.groupWrapper ?: groupWrapper,
        previewWrapper = override.previewWrapper ?: previewWrapper,
        accessibilityWrapper = override.accessibilityWrapper ?: accessibilityWrapper,
        groupOverrides = groupOverrides + override.groupOverrides,
        previewOverrides = previewOverrides + override.previewOverrides,
        localeOptions = override.localeOptions ?: localeOptions,
        themeOptions = themeOptions + override.themeOptions,
        customTypeFields = customTypeFields + override.customTypeFields,
    )
}
