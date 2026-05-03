package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Composable
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.AccessibilityPreviewState
import kotlin.reflect.KClass

/**
 * Builds a [PreviewConfig] using the DSL.
 *
 * Example:
 * ```
 * val config = previewConfig {
 *     fontScale = 1.2f
 *     groupExpansionMode = GroupExpansionMode.INLINE
 *     browserWrapper { content -> MyAppTheme { content() } }
 *     topBarAction("Info", icon = { Icon(Icons.Default.Info, null) }) { showInfo() }
 * }
 * ```
 */
fun previewConfig(block: PreviewConfigBuilder.() -> Unit): PreviewConfig =
    PreviewConfigBuilder().apply(block).build()

fun previewConfigOverride(block: PreviewConfigOverrideBuilder.() -> Unit): PreviewConfigOverride =
    PreviewConfigOverrideBuilder().apply(block).build()

/** Builder for [PreviewConfig]. Use via [previewConfig]. */
class PreviewConfigBuilder {
    /** Initial font scale applied before any user override. */
    var fontScale: Float = 1.0f

    /** Initial UI (density) scale applied before any user override. */
    var uiScale: Float = 1.0f

    /**
     * Overrides the system dark-mode when non-null. `null` (default) honours the system setting.
     * Only consulted when [browserWrapper] is null; custom wrappers receive the resolved value
     * via [tech.lucam.composeum.runtime.ui.LocalIsDarkTheme].
     */
    var isDarkMode: Boolean? = null

    /** Theme id selected before any user override from the settings sheet. */
    var defaultThemeId: String = ThemeOptionDefaults.Classic.id

    /** BCP 47 locale tag (e.g. "en-US", "fr"), or null to inherit the device locale. */
    var locale: String? = null

    /** Default accessibility test-mode state applied before any user override. */
    var accessibilityState: AccessibilityPreviewState = AccessibilityPreviewState()

    /** Whether group descriptions are shown in the group list by default. */
    var showDescriptions: Boolean = true

    /** Whether tag chips are shown on thumbnail cards by default. */
    var showTags: Boolean = true

    /** Whether the param panel is visible on the detail screen. */
    var showParamPanel: Boolean = true

    /** Number of thumbnail columns in the preview grid by default. */
    var thumbnailColumns: Int = 2

    /** Controls whether leaf groups navigate to a subscreen or expand inline. */
    var groupExpansionMode: GroupExpansionMode = GroupExpansionMode.SUBSCREEN

    /**
     * Settings controls to show in the settings sheet. `null` (default) shows all built-ins.
     * Supply a list to pick which appear and in what order; inject custom composables with
     * [SettingItem.Custom] and remove built-ins by omitting their [BuiltInSettingId].
     */
    var settingsItems: List<SettingItem>? = null

    /**
     * Base URL of your source repository (e.g. `"https://gitlab.com/org/repo/-/blob/main"`).
     * When set, the detail screen top bar shows a button that opens
     * `{sourceBaseUrl}/{relativePath}#L{line}` in the browser.
     * When null (default) the button opens an `idea://open` deep link instead.
     */
    var sourceBaseUrl: String? = null

    /**
     * Absolute path prefix to strip from the KSP-captured source path when constructing
     * the URL (e.g. `"/home/user/workspace/my-project"`).
     */
    var sourceStripPrefix: String? = null

    /** Additional source-path prefixes to strip, useful when builds run from different roots. */
    var sourceStripPrefixes: List<String> = emptyList()

    /** Optional initial route string, typically produced by PreviewRoute route helpers. */
    var initialRoute: String? = null

    /**
     * Locale options shown in the settings sheet locale picker.
     * Pass `null` (default) to use the built-in list (System + en, de, fr, es, ja, ar).
     * Each [LocaleOption] has a BCP 47 [LocaleOption.tag] and a [LocaleOption.displayName].
     * Use `"system"` as the tag for the device-default entry.
     */
    var localeOptions: List<LocaleOption>? = null
    var themeOptions: List<ThemeOption> = emptyList()

    private var browserWrapper: BrowserWrapper? = null
    private var onShareableRouteChanged: ((String) -> Unit)? = null
    private var groupWrapper: GroupWrapper? = null
    private var previewWrapper: PreviewWrapper? = null
    private var accessibilityWrapper: AccessibilityWrapper? = null
    private val groupOverrides = mutableMapOf<KClass<out PreviewGroup>, GroupConfig>()
    private val previewOverrides = mutableMapOf<String, PreviewOverride>()
    private val topBarActionsList = mutableListOf<TopBarAction>()
    private val customTypeFieldsMap = mutableMapOf<String, CustomParamField>()
    private val themeOptionsList = mutableListOf<ThemeOption>()

    /** Sets the composable that wraps the entire browser. */
    fun browserWrapper(block: BrowserWrapper) {
        browserWrapper = block
    }

    /** Receives the browser's current shareable route whenever it changes. */
    fun onShareableRouteChanged(block: (String) -> Unit) {
        onShareableRouteChanged = block
    }

    /** Sets the composable that wraps each group's preview list. */
    fun groupWrapper(block: GroupWrapper) {
        groupWrapper = block
    }

    /** Sets the composable that wraps each individual preview card. */
    fun previewWrapper(block: PreviewWrapper) {
        previewWrapper = block
    }

    /** Sets the composable that wraps preview renders with the resolved accessibility state. */
    fun accessibilityWrapper(block: AccessibilityWrapper) {
        accessibilityWrapper = block
    }

    /** Configures per-group overrides. */
    fun groups(block: GroupOverrideBuilder.() -> Unit) {
        GroupOverrideBuilder(groupOverrides).apply(block)
    }

    /** Registers a per-preview override by registry key. */
    fun preview(key: String, block: PreviewOverrideBuilder.() -> Unit) {
        val override = PreviewOverrideBuilder().apply(block).build()
        if (override != PreviewOverride()) {
            previewOverrides[key] = override
        }
    }

    /**
     * Registers a custom param-panel widget for parameters of type [T].
     *
     * The widget replaces the default string text field for any `@PreviewParam`-annotated
     * parameter whose fully-qualified type name matches [T]. The [initialValue] is placed
     * in state before the first render so the composable always receives a typed value.
     *
     * Example:
     * ```kotlin
     * customTypeField<StarRating>(initialValue = StarRating(3)) { value, onValue ->
     *     StarRatingWidget(value, onValue)
     * }
     * ```
     */
    inline fun <reified T : Any> customTypeField(
        initialValue: T,
        noinline widget: @Composable (value: T, onValue: (T) -> Unit) -> Unit,
    ) {
        val typeName = T::class.qualifiedName
            ?: error("Cannot register a customTypeField for an anonymous type")
        addCustomTypeField(
            typeName,
            CustomParamField(
                initialValue = initialValue,
                widget = { v, ov ->
                    @Suppress("UNCHECKED_CAST")
                    widget(v as T) { ov(it) }
                },
            ),
        )
    }

    @PublishedApi
    internal fun addCustomTypeField(typeName: String, field: CustomParamField) {
        customTypeFieldsMap[typeName] = field
    }

    /**
     * Adds a custom icon button to the top app bar (inserted before the settings icon).
     *
     * @param contentDescription Accessibility label.
     * @param icon Composable rendering the icon, e.g. `{ Icon(Icons.Default.Share, null) }`.
     * @param onClick Action invoked on tap.
     */
    fun topBarAction(
        contentDescription: String,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
    ) {
        topBarActionsList.add(TopBarAction(contentDescription, icon, onClick))
    }

    /** Appends a custom theme option to the settings picker. */
    fun themeOption(option: ThemeOption) {
        themeOptionsList.add(option)
    }

    /** Builds the immutable [PreviewConfig]. */
    fun build(): PreviewConfig = PreviewConfig(
        fontScale = fontScale,
        uiScale = uiScale,
        isDarkMode = isDarkMode,
        defaultThemeId = defaultThemeId,
        locale = locale,
        accessibilityState = accessibilityState,
        showDescriptions = showDescriptions,
        showTags = showTags,
        showParamPanel = showParamPanel,
        thumbnailColumns = thumbnailColumns,
        groupExpansionMode = groupExpansionMode,
        settingsItems = settingsItems,
        topBarActions = topBarActionsList.toList(),
        sourceBaseUrl = sourceBaseUrl,
        sourceStripPrefix = sourceStripPrefix,
        sourceStripPrefixes = sourceStripPrefixes,
        initialRoute = initialRoute,
        onShareableRouteChanged = onShareableRouteChanged,
        browserWrapper = browserWrapper,
        groupWrapper = groupWrapper,
        previewWrapper = previewWrapper,
        accessibilityWrapper = accessibilityWrapper,
        groupOverrides = groupOverrides,
        previewOverrides = previewOverrides,
        localeOptions = localeOptions,
        themeOptions = themeOptions + themeOptionsList,
        customTypeFields = customTypeFieldsMap.toMap(),
    )
}

/** Builder for [PreviewConfigOverride]. Use via [previewConfigOverride]. */
class PreviewConfigOverrideBuilder {
    var fontScale: Float? = null
    var uiScale: Float? = null
    var isDarkMode: Boolean? = null
    var defaultThemeId: String? = null
    var locale: String? = null
    var accessibilityState: AccessibilityPreviewState? = null
    var showDescriptions: Boolean? = null
    var showTags: Boolean? = null
    var showParamPanel: Boolean? = null
    var thumbnailColumns: Int? = null
    var groupExpansionMode: GroupExpansionMode? = null
    var settingsItems: List<SettingItem>? = null
    var sourceBaseUrl: String? = null
    var sourceStripPrefix: String? = null
    var sourceStripPrefixes: List<String> = emptyList()
    var initialRoute: String? = null
    var localeOptions: List<LocaleOption>? = null
    var themeOptions: List<ThemeOption> = emptyList()

    private var browserWrapper: BrowserWrapper? = null
    private var onShareableRouteChanged: ((String) -> Unit)? = null
    private var groupWrapper: GroupWrapper? = null
    private var previewWrapper: PreviewWrapper? = null
    private var accessibilityWrapper: AccessibilityWrapper? = null
    private val groupOverrides = mutableMapOf<KClass<out PreviewGroup>, GroupConfig>()
    private val previewOverrides = mutableMapOf<String, PreviewOverride>()
    private val topBarActionsList = mutableListOf<TopBarAction>()
    private val customTypeFieldsMap = mutableMapOf<String, CustomParamField>()
    private val themeOptionsList = mutableListOf<ThemeOption>()

    fun browserWrapper(block: BrowserWrapper) {
        browserWrapper = block
    }

    fun onShareableRouteChanged(block: (String) -> Unit) {
        onShareableRouteChanged = block
    }

    fun groupWrapper(block: GroupWrapper) {
        groupWrapper = block
    }

    fun previewWrapper(block: PreviewWrapper) {
        previewWrapper = block
    }

    fun accessibilityWrapper(block: AccessibilityWrapper) {
        accessibilityWrapper = block
    }

    fun groups(block: GroupOverrideBuilder.() -> Unit) {
        GroupOverrideBuilder(groupOverrides).apply(block)
    }

    fun preview(key: String, block: PreviewOverrideBuilder.() -> Unit) {
        val override = PreviewOverrideBuilder().apply(block).build()
        if (override != PreviewOverride()) {
            previewOverrides[key] = override
        }
    }

    internal inline fun <reified T : Any> customTypeField(
        initialValue: T,
        noinline widget: @Composable (value: T, onValue: (T) -> Unit) -> Unit,
    ) {
        val typeName = T::class.qualifiedName
            ?: error("Cannot register a customTypeField for an anonymous type")
        customTypeFieldsMap[typeName] = CustomParamField(
            initialValue = initialValue,
            widget = { v, ov ->
                @Suppress("UNCHECKED_CAST")
                widget(v as T) { ov(it) }
            },
        )
    }

    fun topBarAction(
        contentDescription: String,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
    ) {
        topBarActionsList.add(TopBarAction(contentDescription, icon, onClick))
    }

    fun themeOption(option: ThemeOption) {
        themeOptionsList.add(option)
    }

    fun build(): PreviewConfigOverride = PreviewConfigOverride(
        fontScale = fontScale,
        uiScale = uiScale,
        isDarkMode = isDarkMode,
        defaultThemeId = defaultThemeId,
        locale = locale,
        accessibilityState = accessibilityState,
        showDescriptions = showDescriptions,
        showTags = showTags,
        showParamPanel = showParamPanel,
        thumbnailColumns = thumbnailColumns,
        groupExpansionMode = groupExpansionMode,
        settingsItems = settingsItems,
        topBarActions = topBarActionsList.toList(),
        sourceBaseUrl = sourceBaseUrl,
        sourceStripPrefix = sourceStripPrefix,
        sourceStripPrefixes = sourceStripPrefixes,
        initialRoute = initialRoute,
        onShareableRouteChanged = onShareableRouteChanged,
        browserWrapper = browserWrapper,
        groupWrapper = groupWrapper,
        previewWrapper = previewWrapper,
        accessibilityWrapper = accessibilityWrapper,
        groupOverrides = groupOverrides,
        previewOverrides = previewOverrides,
        localeOptions = localeOptions,
        themeOptions = themeOptions + themeOptionsList,
        customTypeFields = customTypeFieldsMap.toMap(),
    )
}

/** Configures per-group [GroupConfig] overrides inside [PreviewConfigBuilder.groups]. */
class GroupOverrideBuilder(
    private val overrides: MutableMap<KClass<out PreviewGroup>, GroupConfig>,
) {
    /** Registers a [GroupConfig] override for the given group class. */
    fun group(groupClass: KClass<out PreviewGroup>, block: GroupConfigBuilder.() -> Unit) {
        overrides[groupClass] = GroupConfigBuilder().apply(block).build()
    }

    /** Registers a [GroupConfig] override for the reified group type [G]. */
    inline fun <reified G : PreviewGroup> group(noinline block: GroupConfigBuilder.() -> Unit) {
        group(G::class, block)
    }
}

/** Builder for [GroupConfig]. Use inside [GroupOverrideBuilder.group]. */
class GroupConfigBuilder {
    /** Override the number of thumbnail columns for this group; `null` inherits the global value. */
    var thumbnailColumns: Int? = null

    /** Override how this group behaves when tapped; `null` inherits [PreviewConfigBuilder.groupExpansionMode]. */
    var expansionMode: GroupExpansionMode? = null

    private var groupWrapper: GroupWrapper? = null
    private var previewWrapper: PreviewWrapper? = null

    /** Sets the composable that wraps this group's preview list. */
    fun groupWrapper(block: GroupWrapper) {
        groupWrapper = block
    }

    /** Sets the composable that wraps each preview card in this group. */
    fun previewWrapper(block: PreviewWrapper) {
        previewWrapper = block
    }

    /** Builds the immutable [GroupConfig]. */
    fun build(): GroupConfig = GroupConfig(
        thumbnailColumns = thumbnailColumns,
        expansionMode = expansionMode,
        groupWrapper = groupWrapper,
        previewWrapper = previewWrapper,
    )
}

/** Builder for [PreviewOverride]. Use inside [PreviewConfigBuilder.preview]. */
class PreviewOverrideBuilder {
    var showParamPanel: Boolean? = null

    private var previewWrapper: PreviewWrapper? = null
    private var paramForm: PreviewParamForm? = null
    private var paramDefaults: tech.lucam.composeum.runtime.PreviewParamDefaults? = null

    fun previewWrapper(block: PreviewWrapper) {
        previewWrapper = block
    }

    fun paramForm(block: PreviewParamForm) {
        paramForm = block
    }

    fun params(params: tech.lucam.composeum.runtime.PreviewParamsDsl) {
        paramForm = params.buildParamForm()
        paramDefaults = params.buildDefaults()
    }

    fun params(block: tech.lucam.composeum.runtime.PreviewParamsDsl.() -> Unit) {
        params(tech.lucam.composeum.runtime.previewParams(block))
    }

    fun build(): PreviewOverride = PreviewOverride(
        previewWrapper = previewWrapper,
        showParamPanel = showParamPanel,
        paramForm = paramForm,
        paramDefaults = paramDefaults,
    )
}
