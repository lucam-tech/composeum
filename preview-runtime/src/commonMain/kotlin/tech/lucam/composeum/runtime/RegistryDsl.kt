package tech.lucam.composeum.runtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.annotation.PreviewTag
import tech.lucam.composeum.annotation.PreviewVariantGroup
import tech.lucam.composeum.runtime.config.GroupConfigBuilder
import tech.lucam.composeum.runtime.config.PreviewConfigOverride
import tech.lucam.composeum.runtime.config.PreviewConfigOverrideBuilder
import tech.lucam.composeum.runtime.config.PreviewParamForm
import tech.lucam.composeum.runtime.config.PreviewOverrideBuilder
import tech.lucam.composeum.runtime.config.asOverride
import tech.lucam.composeum.runtime.config.mergedWith
import tech.lucam.composeum.runtime.ui.component.LocalPreviewParamState
import tech.lucam.composeum.runtime.ui.widgets.PreviewAlignmentField
import tech.lucam.composeum.runtime.ui.widgets.PreviewBooleanField
import tech.lucam.composeum.runtime.ui.widgets.PreviewColorField
import tech.lucam.composeum.runtime.ui.widgets.PreviewContentSlotField
import tech.lucam.composeum.runtime.ui.widgets.PreviewDpField
import tech.lucam.composeum.runtime.ui.widgets.PreviewDropdownField
import tech.lucam.composeum.runtime.ui.widgets.PreviewFloatField
import tech.lucam.composeum.runtime.ui.widgets.PreviewIntField
import tech.lucam.composeum.runtime.ui.widgets.PreviewOptionChipsField
import tech.lucam.composeum.runtime.ui.widgets.PreviewPaddingValuesField
import tech.lucam.composeum.runtime.ui.widgets.PreviewStringField
import tech.lucam.composeum.runtime.ui.widgets.PreviewTextUnitField

/** DSL scope marker — prevents implicit use of outer receivers inside nested DSL blocks. */
@DslMarker
annotation class PreviewRegistryDsl

/**
 * Builds a [PreviewRegistry] using the DSL.
 *
 * Supports:
 * - flat preview registration
 * - nested `group { ... }` blocks that set the default group
 * - including other [PreviewRegistry] instances
 * - inline registry-local [PreviewConfig] and group/preview overrides
 */
fun buildRegistry(block: RegistryBuilder.() -> Unit): PreviewRegistry =
    RegistryBuilder().apply(block).build()

/**
 * Builds a [PreviewParamsDsl] for use as the `params` argument in [RegistryBuilder.preview].
 *
 * Each method call inside the block registers one interactive parameter with a key,
 * default value, and the corresponding widget shown in the param panel.
 */
fun previewParams(block: PreviewParamsDsl.() -> Unit): PreviewParamsDsl =
    PreviewParamsDsl().apply(block)

/** Builder used inside [buildRegistry]. */
@PreviewRegistryDsl
class RegistryBuilder {

    private val entries = mutableListOf<PreviewEntry>()
    private val includedConfigOverrides = mutableListOf<PreviewConfigOverride>()
    private val configBuilder = PreviewConfigOverrideBuilder()

    /** Applies registry-local browser configuration. */
    fun config(block: PreviewConfigOverrideBuilder.() -> Unit) {
        configBuilder.apply(block)
    }

    /** Configures per-group overrides for this registry. */
    fun groups(block: tech.lucam.composeum.runtime.config.GroupOverrideBuilder.() -> Unit) {
        configBuilder.groups(block)
    }

    /** Registers a per-preview override by preview key. */
    fun previewOverride(key: String, block: PreviewOverrideBuilder.() -> Unit) {
        configBuilder.preview(key, block)
    }

    /** Includes another registry's entries and configuration. */
    fun include(registry: PreviewRegistry) {
        entries += registry.entries
        includedConfigOverrides += registry.config.asOverride().mergedWith(registry.configOverride)
    }

    /** Alias for [include]. */
    fun registry(registry: PreviewRegistry) {
        include(registry)
    }

    /** Opens a nested scope where [group] becomes the default for contained previews. */
    fun group(
        group: PreviewGroup,
        configure: GroupConfigBuilder.() -> Unit = {},
        block: GroupScope.() -> Unit,
    ) {
        val builder = GroupConfigBuilder().apply(configure).build()
        if (builder != tech.lucam.composeum.runtime.config.GroupConfig()) {
            configBuilder.groups {
                group(group::class) { 
                    thumbnailColumns = builder.thumbnailColumns
                    expansionMode = builder.expansionMode
                    builder.groupWrapper?.let { groupWrapper(it) }
                    builder.previewWrapper?.let { previewWrapper(it) }
                }
            }
        }
        GroupScope(this, group).apply(block)
    }

    /**
     * Registers a simple preview with no interactive parameters.
     *
     * Flat form retained for backward compatibility.
     */
    fun preview(
        name: String,
        group: PreviewGroup,
        variantGroup: PreviewVariantGroup? = null,
        isDefaultVariant: Boolean = false,
        description: String = "",
        tags: List<PreviewTag> = emptyList(),
        key: String = defaultKey(group, name),
        configure: PreviewOverrideBuilder.() -> Unit = {},
        composable: @Composable () -> Unit,
    ) {
        registerPreview(
            group = group,
            name = name,
            variantGroup = variantGroup,
            isDefaultVariant = isDefaultVariant,
            description = description,
            tags = tags,
            key = key,
            paramForm = null,
            paramDefaults = PreviewParamDefaults(emptyMap()),
            configure = configure,
            composable = composable,
        )
    }

    /**
     * Registers a parameterized preview whose render lambda receives the live [PreviewParamState].
     *
     * Flat form retained for backward compatibility.
     */
    fun preview(
        name: String,
        group: PreviewGroup,
        params: PreviewParamsDsl,
        variantGroup: PreviewVariantGroup? = null,
        isDefaultVariant: Boolean = false,
        description: String = "",
        tags: List<PreviewTag> = emptyList(),
        key: String = defaultKey(group, name),
        configure: PreviewOverrideBuilder.() -> Unit = {},
        composable: @Composable (PreviewParamState) -> Unit,
    ) {
        registerPreview(
            group = group,
            name = name,
            variantGroup = variantGroup,
            isDefaultVariant = isDefaultVariant,
            description = description,
            tags = tags,
            key = key,
            paramForm = params.buildParamForm(),
            paramDefaults = params.buildDefaults(),
            configure = configure,
            composable = {
                val state = LocalPreviewParamState.current
                composable(state)
            },
        )
    }

    internal fun registerPreview(
        group: PreviewGroup,
        name: String,
        variantGroup: PreviewVariantGroup?,
        isDefaultVariant: Boolean,
        description: String,
        tags: List<PreviewTag>,
        key: String,
        paramForm: PreviewParamForm?,
        paramDefaults: PreviewParamDefaults,
        configure: PreviewOverrideBuilder.() -> Unit,
        composable: @Composable () -> Unit,
    ) {
        require(variantGroup != null || !isDefaultVariant) {
            "Preview '$key' sets isDefaultVariant=true but does not declare a variantGroup."
        }
        entries += PreviewEntry(
            key = key,
            name = name,
            group = group,
            variantGroup = variantGroup,
            isDefaultVariant = isDefaultVariant,
            description = description,
            tags = tags,
            composable = composable,
            paramForm = paramForm,
            paramDefaults = paramDefaults,
        )
        configBuilder.preview(key, configure)
    }

    internal fun build(): PreviewRegistry {
        val localConfigOverride = configBuilder.build()
        val mergedConfigOverride = includedConfigOverrides.fold(PreviewConfigOverride()) { acc, next ->
            acc.mergedWith(next)
        }.mergedWith(localConfigOverride)
        val builtEntries = entries.toList().distinctBy { it.key }
        return object : PreviewRegistry {
            override val entries: List<PreviewEntry> = builtEntries
            override val configOverride: PreviewConfigOverride = mergedConfigOverride
        }
    }

    internal fun defaultKey(group: PreviewGroup, name: String): String = "${group.groupKey()}/$name"
}

/** Nested DSL scope with a default [PreviewGroup]. */
@PreviewRegistryDsl
class GroupScope internal constructor(
    private val parent: RegistryBuilder,
    private val defaultGroup: PreviewGroup,
) {
    fun config(block: PreviewConfigOverrideBuilder.() -> Unit) {
        parent.config(block)
    }

    fun previewOverride(key: String, block: PreviewOverrideBuilder.() -> Unit) {
        parent.previewOverride(key, block)
    }

    fun include(registry: PreviewRegistry) {
        parent.include(registry)
    }

    fun registry(registry: PreviewRegistry) {
        parent.include(registry)
    }

    fun group(
        group: PreviewGroup,
        configure: GroupConfigBuilder.() -> Unit = {},
        block: GroupScope.() -> Unit,
    ) {
        parent.group(group, configure, block)
    }

    fun preview(
        name: String,
        variantGroup: PreviewVariantGroup? = null,
        isDefaultVariant: Boolean = false,
        description: String = "",
        tags: List<PreviewTag> = emptyList(),
        key: String = "${defaultGroup.groupKey()}/$name",
        configure: PreviewOverrideBuilder.() -> Unit = {},
        composable: @Composable () -> Unit,
    ) {
        parent.preview(
            name = name,
            group = defaultGroup,
            variantGroup = variantGroup,
            isDefaultVariant = isDefaultVariant,
            description = description,
            tags = tags,
            key = key,
            configure = configure,
            composable = composable,
        )
    }

    fun preview(
        name: String,
        params: PreviewParamsDsl,
        variantGroup: PreviewVariantGroup? = null,
        isDefaultVariant: Boolean = false,
        description: String = "",
        tags: List<PreviewTag> = emptyList(),
        key: String = "${defaultGroup.groupKey()}/$name",
        configure: PreviewOverrideBuilder.() -> Unit = {},
        composable: @Composable (PreviewParamState) -> Unit,
    ) {
        parent.preview(
            name = name,
            group = defaultGroup,
            params = params,
            variantGroup = variantGroup,
            isDefaultVariant = isDefaultVariant,
            description = description,
            tags = tags,
            key = key,
            configure = configure,
            composable = composable,
        )
    }
}

/** A single param definition inside [PreviewParamsDsl]. */
internal class ParamDef(
    val key: String,
    val defaultValue: Any,
    val widget: @Composable (PreviewParamState, (PreviewParamState) -> Unit) -> Unit,
)

/**
 * DSL for declaring interactive preview parameters. Obtain via [previewParams].
 *
 * Each method registers one parameter: a unique [key] used for state look-up,
 * a [default] value stored in [PreviewParamDefaults], and the widget rendered
 * in the param panel.
 */
@PreviewRegistryDsl
class PreviewParamsDsl {

    private val defs = mutableListOf<ParamDef>()

    /** Registers a String text-field parameter. */
    fun string(
        key: String,
        default: String = "",
        label: String = key,
        description: String = "",
    ): PreviewParamKey<String> {
        val paramKey = previewParamKey<String>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: String = state[paramKey] ?: default
            PreviewStringField(
                label = label,
                value = value,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a Boolean toggle parameter. */
    fun boolean(
        key: String,
        default: Boolean = false,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<Boolean> {
        val paramKey = previewParamKey<Boolean>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: Boolean = state[paramKey] ?: default
            PreviewBooleanField(
                label = label,
                value = value,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers an Int slider parameter. */
    fun int(
        key: String,
        default: Int = 0,
        label: String = key,
        range: IntRange = 0..100,
        description: String = "",
    ): PreviewParamKey<Int> {
        val paramKey = previewParamKey<Int>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: Int = state[paramKey] ?: default
            PreviewIntField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a Float slider parameter. */
    fun float(
        key: String,
        default: Float = 0f,
        label: String = key,
        range: ClosedFloatingPointRange<Float> = 0f..1f,
        description: String = "",
    ): PreviewParamKey<Float> {
        val paramKey = previewParamKey<Float>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: Float = state[paramKey] ?: default
            PreviewFloatField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a [Color] swatch-picker parameter. */
    fun color(
        key: String,
        default: Color = Color.Unspecified,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<Color> {
        val paramKey = previewParamKey<Color>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: Color = state[paramKey] ?: default
            PreviewColorField(
                label = label,
                value = value,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a [Dp] slider parameter. */
    fun dp(
        key: String,
        default: Dp = 0.dp,
        label: String = key,
        range: ClosedFloatingPointRange<Float> = 0f..512f,
        description: String = "",
    ): PreviewParamKey<Dp> {
        val paramKey = previewParamKey<Dp>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: Dp = state[paramKey] ?: default
            PreviewDpField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a [TextUnit] (sp) slider parameter. */
    fun textUnit(
        key: String,
        default: TextUnit = 16.sp,
        label: String = key,
        range: ClosedFloatingPointRange<Float> = 8f..64f,
        description: String = "",
    ): PreviewParamKey<TextUnit> {
        val paramKey = previewParamKey<TextUnit>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: TextUnit = state[paramKey] ?: default
            PreviewTextUnitField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a String dropdown parameter. */
    fun dropdown(
        key: String,
        options: List<String>,
        default: String = options.first(),
        label: String = key,
        description: String = "",
    ): PreviewParamKey<String> {
        val paramKey = previewParamKey<String>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: String = state[paramKey] ?: default
            PreviewDropdownField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a 2D [Alignment] parameter rendered as a 3×3 grid picker. */
    fun alignment(
        key: String,
        default: Alignment = Alignment.Center,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<Alignment> {
        val paramKey = previewParamKey<Alignment>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: Alignment = state[paramKey] ?: default
            PreviewAlignmentField(
                label = label,
                value = value,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers an [Alignment.Horizontal] parameter rendered as an option chip row. */
    fun alignmentHorizontal(
        key: String,
        default: Alignment.Horizontal = Alignment.Start,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<Alignment.Horizontal> {
        val paramKey = previewParamKey<Alignment.Horizontal>(key)
        val options = listOf(
            "Start" to Alignment.Start,
            "Center" to Alignment.CenterHorizontally,
            "End" to Alignment.End,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: Alignment.Horizontal = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers an [Alignment.Vertical] parameter rendered as an option chip row. */
    fun alignmentVertical(
        key: String,
        default: Alignment.Vertical = Alignment.Top,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<Alignment.Vertical> {
        val paramKey = previewParamKey<Alignment.Vertical>(key)
        val options = listOf(
            "Top" to Alignment.Top,
            "Center" to Alignment.CenterVertically,
            "Bottom" to Alignment.Bottom,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: Alignment.Vertical = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers an [Arrangement.Horizontal] parameter rendered as an option chip row. */
    fun arrangementHorizontal(
        key: String,
        default: Arrangement.Horizontal = Arrangement.Start,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<Arrangement.Horizontal> {
        val paramKey = previewParamKey<Arrangement.Horizontal>(key)
        val options = listOf(
            "Start" to Arrangement.Start,
            "Center" to Arrangement.Center,
            "End" to Arrangement.End,
            "Space Between" to Arrangement.SpaceBetween,
            "Space Around" to Arrangement.SpaceAround,
            "Space Evenly" to Arrangement.SpaceEvenly,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: Arrangement.Horizontal = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers an [Arrangement.Vertical] parameter rendered as an option chip row. */
    fun arrangementVertical(
        key: String,
        default: Arrangement.Vertical = Arrangement.Top,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<Arrangement.Vertical> {
        val paramKey = previewParamKey<Arrangement.Vertical>(key)
        val options = listOf(
            "Top" to Arrangement.Top,
            "Center" to Arrangement.Center,
            "Bottom" to Arrangement.Bottom,
            "Space Between" to Arrangement.SpaceBetween,
            "Space Around" to Arrangement.SpaceAround,
            "Space Evenly" to Arrangement.SpaceEvenly,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: Arrangement.Vertical = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /**
     * Registers a content slot parameter.
     *
     * [options] is a list of (display name, composable) pairs. The selected composable
     * is retrieved via [ContentSlotValue.content] on the value stored in [PreviewParamState].
     *
     * Content slot values are in-memory only and are never persisted across sessions.
     *
     * @param defaultIndex Index into [options] used as the initial selection; defaults to 0.
     */
    fun contentSlot(
        key: String,
        options: List<Pair<String, @Composable () -> Unit>>,
        defaultIndex: Int = 0,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<ContentSlotValue> {
        val paramKey = previewParamKey<ContentSlotValue>(key)
        require(options.isNotEmpty()) { "contentSlot '$key' must have at least one option" }
        val safeDefault = defaultIndex.coerceIn(options.indices)
        val default = ContentSlotValue(options, safeDefault)
        defs += ParamDef(key, default) { state, onState ->
            val value: ContentSlotValue = state[paramKey] ?: default
            PreviewContentSlotField(
                label = label,
                optionNames = value.optionNames,
                selectedIndex = value.selectedIndex,
                description = description,
                onIndex = { onState(state.put(paramKey, ContentSlotValue(options, it))) },
            )
        }
        return paramKey
    }

    /** Registers a [FontWeight] parameter rendered as an option chip row. */
    fun fontWeight(
        key: String,
        default: FontWeight = FontWeight.Normal,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<FontWeight> {
        val paramKey = previewParamKey<FontWeight>(key)
        val options = listOf(
            "Thin" to FontWeight.Thin,
            "ExtraLight" to FontWeight.ExtraLight,
            "Light" to FontWeight.Light,
            "Normal" to FontWeight.Normal,
            "Medium" to FontWeight.Medium,
            "SemiBold" to FontWeight.SemiBold,
            "Bold" to FontWeight.Bold,
            "ExtraBold" to FontWeight.ExtraBold,
            "Black" to FontWeight.Black,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: FontWeight = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a [TextAlign] parameter rendered as an option chip row. */
    fun textAlign(
        key: String,
        default: TextAlign = TextAlign.Start,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<TextAlign> {
        val paramKey = previewParamKey<TextAlign>(key)
        val options = listOf(
            "Start" to TextAlign.Start,
            "Center" to TextAlign.Center,
            "End" to TextAlign.End,
            "Justify" to TextAlign.Justify,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: TextAlign = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /**
     * Registers a corner-radius slider parameter for shape previewing.
     *
     * The state value under [key] is a [Dp] corner radius. Wrap it in
     * [RoundedCornerShape] to obtain a [Shape]:
     * ```
     * val radius: Dp = state["shape"] ?: 8.dp
     * Card(shape = RoundedCornerShape(radius)) { … }
     * ```
     *
     * @param default Corner radius used as the initial value; defaults to 8.dp.
     * @param range   Inclusive slider range in raw float dp; defaults to 0..64.
     */
    fun shape(
        key: String,
        default: Dp = 8.dp,
        label: String = key,
        range: ClosedFloatingPointRange<Float> = 0f..64f,
        description: String = "",
    ): PreviewParamKey<Dp> {
        val paramKey = previewParamKey<Dp>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: Dp = state[paramKey] ?: default
            PreviewDpField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a [ContentScale] parameter rendered as an option chip row. */
    fun contentScale(
        key: String,
        default: ContentScale = ContentScale.Fit,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<ContentScale> {
        val paramKey = previewParamKey<ContentScale>(key)
        val options = listOf(
            "Fit" to ContentScale.Fit,
            "Crop" to ContentScale.Crop,
            "Fill Bounds" to ContentScale.FillBounds,
            "Fill Width" to ContentScale.FillWidth,
            "Fill Height" to ContentScale.FillHeight,
            "Inside" to ContentScale.Inside,
            "None" to ContentScale.None,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: ContentScale = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a [LayoutDirection] parameter rendered as an option chip row (Ltr / Rtl). */
    fun layoutDirection(
        key: String,
        default: LayoutDirection = LayoutDirection.Ltr,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<LayoutDirection> {
        val paramKey = previewParamKey<LayoutDirection>(key)
        val options = listOf(
            "LTR" to LayoutDirection.Ltr,
            "RTL" to LayoutDirection.Rtl,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: LayoutDirection = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /**
     * Registers a [PreviewPaddingValues] parameter rendered as four [Dp] sliders.
     *
     * Retrieve the value via `state["key"] as PreviewPaddingValues` and call
     * [PreviewPaddingValues.toPaddingValues] to use it in a composable.
     *
     * @param range Inclusive slider range in raw float dp for each side; defaults to 0..128.
     */
    fun paddingValues(
        key: String,
        default: PreviewPaddingValues = PreviewPaddingValues(),
        label: String = key,
        range: ClosedFloatingPointRange<Float> = 0f..128f,
        description: String = "",
    ): PreviewParamKey<PreviewPaddingValues> {
        val paramKey = previewParamKey<PreviewPaddingValues>(key)
        defs += ParamDef(key, default) { state, onState ->
            val value: PreviewPaddingValues = state[paramKey] ?: default
            PreviewPaddingValuesField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    /** Registers a [FontFamily] parameter rendered as an option chip row. */
    fun fontFamily(
        key: String,
        default: FontFamily = FontFamily.Default,
        label: String = key,
        description: String = "",
    ): PreviewParamKey<FontFamily> {
        val paramKey = previewParamKey<FontFamily>(key)
        val options = listOf(
            "Default" to FontFamily.Default,
            "Serif" to FontFamily.Serif,
            "Sans-Serif" to FontFamily.SansSerif,
            "Monospace" to FontFamily.Monospace,
            "Cursive" to FontFamily.Cursive,
        )
        defs += ParamDef(key, default) { state, onState ->
            val value: FontFamily = state[paramKey] ?: default
            PreviewOptionChipsField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(paramKey, it)) },
            )
        }
        return paramKey
    }

    internal fun buildDefaults(): PreviewParamDefaults =
        PreviewParamDefaults(defaults = defs.associate { it.key to it.defaultValue })

    internal fun buildParamForm(): @Composable (PreviewParamState, (PreviewParamState) -> Unit) -> Unit =
        { state, onState -> defs.forEach { def -> def.widget(state, onState) } }
}
