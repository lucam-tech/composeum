package tech.lucam.composeum.runtime

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.annotation.PreviewTag
import tech.lucam.composeum.runtime.config.GroupConfigBuilder
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.PreviewConfigBuilder
import tech.lucam.composeum.runtime.config.PreviewOverrideBuilder
import tech.lucam.composeum.runtime.config.mergedWith
import tech.lucam.composeum.runtime.ui.component.LocalPreviewParamState
import tech.lucam.composeum.runtime.ui.widgets.PreviewBooleanField
import tech.lucam.composeum.runtime.ui.widgets.PreviewColorField
import tech.lucam.composeum.runtime.ui.widgets.PreviewDpField
import tech.lucam.composeum.runtime.ui.widgets.PreviewDropdownField
import tech.lucam.composeum.runtime.ui.widgets.PreviewFloatField
import tech.lucam.composeum.runtime.ui.widgets.PreviewIntField
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
    private val includedConfigs = mutableListOf<PreviewConfig>()
    private val configBuilder = PreviewConfigBuilder()

    /** Applies registry-local browser configuration. */
    fun config(block: PreviewConfigBuilder.() -> Unit) {
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
        includedConfigs += registry.config
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
        description: String = "",
        tags: List<PreviewTag> = emptyList(),
        key: String = defaultKey(group, name),
        configure: PreviewOverrideBuilder.() -> Unit = {},
        composable: @Composable () -> Unit,
    ) {
        registerPreview(
            group = group,
            name = name,
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
        description: String = "",
        tags: List<PreviewTag> = emptyList(),
        key: String = defaultKey(group, name),
        configure: PreviewOverrideBuilder.() -> Unit = {},
        composable: @Composable (PreviewParamState) -> Unit,
    ) {
        registerPreview(
            group = group,
            name = name,
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
        description: String,
        tags: List<PreviewTag>,
        key: String,
        paramForm: (@Composable (PreviewParamState, (PreviewParamState) -> Unit) -> Unit)?,
        paramDefaults: PreviewParamDefaults,
        configure: PreviewOverrideBuilder.() -> Unit,
        composable: @Composable () -> Unit,
    ) {
        entries += PreviewEntry(
            key = key,
            name = name,
            group = group,
            description = description,
            tags = tags,
            composable = composable,
            paramForm = paramForm,
            paramDefaults = paramDefaults,
        )
        configBuilder.preview(key, configure)
    }

    internal fun build(): PreviewRegistry {
        val localConfig = configBuilder.build()
        val mergedConfig = includedConfigs.fold(PreviewConfig()) { acc, next -> acc.mergedWith(next) }
            .mergedWith(localConfig)
        return object : PreviewRegistry {
            override val entries: List<PreviewEntry> = entries.toList().distinctBy { it.key }
            override val config: PreviewConfig = mergedConfig
        }
    }

    internal fun defaultKey(group: PreviewGroup, name: String): String = "${group.name}/$name"
}

/** Nested DSL scope with a default [PreviewGroup]. */
@PreviewRegistryDsl
class GroupScope internal constructor(
    private val parent: RegistryBuilder,
    private val defaultGroup: PreviewGroup,
) {
    fun config(block: PreviewConfigBuilder.() -> Unit) {
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
        description: String = "",
        tags: List<PreviewTag> = emptyList(),
        key: String = "${defaultGroup.name}/$name",
        configure: PreviewOverrideBuilder.() -> Unit = {},
        composable: @Composable () -> Unit,
    ) {
        parent.preview(
            name = name,
            group = defaultGroup,
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
        description: String = "",
        tags: List<PreviewTag> = emptyList(),
        key: String = "${defaultGroup.name}/$name",
        configure: PreviewOverrideBuilder.() -> Unit = {},
        composable: @Composable (PreviewParamState) -> Unit,
    ) {
        parent.preview(
            name = name,
            group = defaultGroup,
            params = params,
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
    ) {
        defs += ParamDef(key, default) { state, onState ->
            val value: String = state[key] ?: default
            PreviewStringField(
                label = label,
                value = value,
                description = description,
                onValue = { onState(state.put(key, it)) },
            )
        }
    }

    /** Registers a Boolean toggle parameter. */
    fun boolean(
        key: String,
        default: Boolean = false,
        label: String = key,
        description: String = "",
    ) {
        defs += ParamDef(key, default) { state, onState ->
            val value: Boolean = state[key] ?: default
            PreviewBooleanField(
                label = label,
                value = value,
                description = description,
                onValue = { onState(state.put(key, it)) },
            )
        }
    }

    /** Registers an Int slider parameter. */
    fun int(
        key: String,
        default: Int = 0,
        label: String = key,
        range: IntRange = 0..100,
        description: String = "",
    ) {
        defs += ParamDef(key, default) { state, onState ->
            val value: Int = state[key] ?: default
            PreviewIntField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(key, it)) },
            )
        }
    }

    /** Registers a Float slider parameter. */
    fun float(
        key: String,
        default: Float = 0f,
        label: String = key,
        range: ClosedFloatingPointRange<Float> = 0f..1f,
        description: String = "",
    ) {
        defs += ParamDef(key, default) { state, onState ->
            val value: Float = state[key] ?: default
            PreviewFloatField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(key, it)) },
            )
        }
    }

    /** Registers a [Color] swatch-picker parameter. */
    fun color(
        key: String,
        default: Color = Color.Unspecified,
        label: String = key,
        description: String = "",
    ) {
        defs += ParamDef(key, default) { state, onState ->
            val value: Color = state[key] ?: default
            PreviewColorField(
                label = label,
                value = value,
                description = description,
                onValue = { onState(state.put(key, it)) },
            )
        }
    }

    /** Registers a [Dp] slider parameter. */
    fun dp(
        key: String,
        default: Dp = 0.dp,
        label: String = key,
        range: ClosedFloatingPointRange<Float> = 0f..512f,
        description: String = "",
    ) {
        defs += ParamDef(key, default) { state, onState ->
            val value: Dp = state[key] ?: default
            PreviewDpField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(key, it)) },
            )
        }
    }

    /** Registers a [TextUnit] (sp) slider parameter. */
    fun textUnit(
        key: String,
        default: TextUnit = 16.sp,
        label: String = key,
        range: ClosedFloatingPointRange<Float> = 8f..64f,
        description: String = "",
    ) {
        defs += ParamDef(key, default) { state, onState ->
            val value: TextUnit = state[key] ?: default
            PreviewTextUnitField(
                label = label,
                value = value,
                range = range,
                description = description,
                onValue = { onState(state.put(key, it)) },
            )
        }
    }

    /** Registers a String dropdown parameter. */
    fun dropdown(
        key: String,
        options: List<String>,
        default: String = options.first(),
        label: String = key,
        description: String = "",
    ) {
        defs += ParamDef(key, default) { state, onState ->
            val value: String = state[key] ?: default
            PreviewDropdownField(
                label = label,
                value = value,
                options = options,
                description = description,
                onValue = { onState(state.put(key, it)) },
            )
        }
    }

    internal fun buildDefaults(): PreviewParamDefaults =
        PreviewParamDefaults(defaults = defs.associate { it.key to it.defaultValue })

    internal fun buildParamForm(): @Composable (PreviewParamState, (PreviewParamState) -> Unit) -> Unit =
        { state, onState -> defs.forEach { def -> def.widget(state, onState) } }
}
