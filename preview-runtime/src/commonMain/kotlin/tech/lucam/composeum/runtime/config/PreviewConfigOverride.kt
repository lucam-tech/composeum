package tech.lucam.composeum.runtime.config

import tech.lucam.composeum.annotation.PreviewGroup
import kotlin.reflect.KClass

/**
 * Explicit browser configuration overrides layered on top of a concrete [PreviewConfig].
 *
 * Scalar/object nulls mean "leave the current value unchanged". Collection fields remain
 * additive so independent registries can contribute actions, themes, wrappers, and
 * per-group overrides without depending on library defaults.
 */
data class PreviewConfigOverride(
    val fontScale: Float? = null,
    val uiScale: Float? = null,
    val isDarkMode: Boolean? = null,
    val defaultThemeId: String? = null,
    val locale: String? = null,
    val showDescriptions: Boolean? = null,
    val showTags: Boolean? = null,
    val showParamPanel: Boolean? = null,
    val thumbnailColumns: Int? = null,
    val groupExpansionMode: GroupExpansionMode? = null,
    val settingsItems: List<SettingItem>? = null,
    val topBarActions: List<TopBarAction> = emptyList(),
    val sourceBaseUrl: String? = null,
    val sourceStripPrefix: String? = null,
    val sourceStripPrefixes: List<String> = emptyList(),
    val initialRoute: String? = null,
    val onShareableRouteChanged: ((String) -> Unit)? = null,
    val browserWrapper: BrowserWrapper? = null,
    val groupWrapper: GroupWrapper? = null,
    val previewWrapper: PreviewWrapper? = null,
    val groupOverrides: Map<KClass<out PreviewGroup>, GroupConfig> = emptyMap(),
    val previewOverrides: Map<String, PreviewOverride> = emptyMap(),
    val localeOptions: List<LocaleOption>? = null,
    val themeOptions: List<ThemeOption> = emptyList(),
    val customTypeFields: Map<String, CustomParamField> = emptyMap(),
)

internal fun PreviewConfigOverride.mergedWith(override: PreviewConfigOverride): PreviewConfigOverride =
    PreviewConfigOverride(
        fontScale = override.fontScale ?: fontScale,
        uiScale = override.uiScale ?: uiScale,
        isDarkMode = override.isDarkMode ?: isDarkMode,
        defaultThemeId = override.defaultThemeId ?: defaultThemeId,
        locale = override.locale ?: locale,
        showDescriptions = override.showDescriptions ?: showDescriptions,
        showTags = override.showTags ?: showTags,
        showParamPanel = override.showParamPanel ?: showParamPanel,
        thumbnailColumns = override.thumbnailColumns ?: thumbnailColumns,
        groupExpansionMode = override.groupExpansionMode ?: groupExpansionMode,
        settingsItems = override.settingsItems ?: settingsItems,
        topBarActions = topBarActions + override.topBarActions,
        sourceBaseUrl = override.sourceBaseUrl ?: sourceBaseUrl,
        sourceStripPrefix = override.sourceStripPrefix ?: sourceStripPrefix,
        sourceStripPrefixes = sourceStripPrefixes + override.sourceStripPrefixes,
        initialRoute = override.initialRoute ?: initialRoute,
        onShareableRouteChanged = override.onShareableRouteChanged ?: onShareableRouteChanged,
        browserWrapper = override.browserWrapper ?: browserWrapper,
        groupWrapper = override.groupWrapper ?: groupWrapper,
        previewWrapper = override.previewWrapper ?: previewWrapper,
        groupOverrides = groupOverrides + override.groupOverrides,
        previewOverrides = previewOverrides + override.previewOverrides,
        localeOptions = override.localeOptions ?: localeOptions,
        themeOptions = themeOptions + override.themeOptions,
        customTypeFields = customTypeFields + override.customTypeFields,
    )

internal fun PreviewConfig.overriddenBy(override: PreviewConfigOverride): PreviewConfig =
    PreviewConfig(
        fontScale = override.fontScale ?: fontScale,
        uiScale = override.uiScale ?: uiScale,
        isDarkMode = override.isDarkMode ?: isDarkMode,
        defaultThemeId = override.defaultThemeId ?: defaultThemeId,
        locale = override.locale ?: locale,
        showDescriptions = override.showDescriptions ?: showDescriptions,
        showTags = override.showTags ?: showTags,
        showParamPanel = override.showParamPanel ?: showParamPanel,
        thumbnailColumns = override.thumbnailColumns ?: thumbnailColumns,
        groupExpansionMode = override.groupExpansionMode ?: groupExpansionMode,
        settingsItems = override.settingsItems ?: settingsItems,
        topBarActions = topBarActions + override.topBarActions,
        sourceBaseUrl = override.sourceBaseUrl ?: sourceBaseUrl,
        sourceStripPrefix = override.sourceStripPrefix ?: sourceStripPrefix,
        sourceStripPrefixes = sourceStripPrefixes + override.sourceStripPrefixes,
        initialRoute = override.initialRoute ?: initialRoute,
        onShareableRouteChanged = override.onShareableRouteChanged ?: onShareableRouteChanged,
        browserWrapper = override.browserWrapper ?: browserWrapper,
        groupWrapper = override.groupWrapper ?: groupWrapper,
        previewWrapper = override.previewWrapper ?: previewWrapper,
        groupOverrides = groupOverrides + override.groupOverrides,
        previewOverrides = previewOverrides + override.previewOverrides,
        localeOptions = override.localeOptions ?: localeOptions,
        themeOptions = themeOptions + override.themeOptions,
        customTypeFields = customTypeFields + override.customTypeFields,
    )

internal fun PreviewConfig.asOverride(): PreviewConfigOverride = PreviewConfigOverride(
    fontScale = fontScale,
    uiScale = uiScale,
    isDarkMode = isDarkMode,
    defaultThemeId = defaultThemeId,
    locale = locale,
    showDescriptions = showDescriptions,
    showTags = showTags,
    showParamPanel = showParamPanel,
    thumbnailColumns = thumbnailColumns,
    groupExpansionMode = groupExpansionMode,
    settingsItems = settingsItems,
    topBarActions = topBarActions,
    sourceBaseUrl = sourceBaseUrl,
    sourceStripPrefix = sourceStripPrefix,
    sourceStripPrefixes = sourceStripPrefixes,
    initialRoute = initialRoute,
    onShareableRouteChanged = onShareableRouteChanged,
    browserWrapper = browserWrapper,
    groupWrapper = groupWrapper,
    previewWrapper = previewWrapper,
    groupOverrides = groupOverrides,
    previewOverrides = previewOverrides,
    localeOptions = localeOptions,
    themeOptions = themeOptions,
    customTypeFields = customTypeFields,
)
