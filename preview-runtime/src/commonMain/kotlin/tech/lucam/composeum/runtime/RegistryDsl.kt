package tech.lucam.composeum.runtime

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.lucam.composeum.annotation.PreviewGroup
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
 * Example:
 * ```kotlin
 * val myRegistry = buildRegistry {
 *     preview(name = "Greeting", group = MyGroup.Components) {
 *         Text("Hello, World!")
 *     }
 *
 *     preview(
 *         name = "Button",
 *         group = MyGroup.Components,
 *         params = previewParams {
 *             string(key = "label", default = "Click me", label = "Button Label")
 *             boolean(key = "enabled", default = true, label = "Enabled")
 *         },
 *     ) { state ->
 *         Button(enabled = state["enabled"] ?: true, onClick = {}) {
 *             Text(state["label"] ?: "Click me")
 *         }
 *     }
 * }
 * ```
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

/** Builder used inside [buildRegistry]. Call [preview] one or more times to register entries. */
@PreviewRegistryDsl
class RegistryBuilder {

    private val _entries = mutableListOf<PreviewEntry>()

    /**
     * Registers a simple preview with no interactive parameters.
     *
     * @param name Display name shown in the browser list.
     * @param group Group this preview belongs to.
     * @param description Optional subtitle shown on the detail screen.
     * @param tags Searchable tags.
     * @param key Unique deduplication key. Defaults to `"${group.name}/$name"`.
     * @param composable The composable to render.
     */
    fun preview(
        name: String,
        group: PreviewGroup,
        description: String = "",
        tags: List<String> = emptyList(),
        key: String = "${group.name}/$name",
        composable: @Composable () -> Unit,
    ) {
        _entries += PreviewEntry(
            key = key,
            name = name,
            group = group,
            description = description,
            tags = tags,
            composable = composable,
            paramForm = null,
            paramDefaults = PreviewParamDefaults(emptyMap()),
        )
    }

    /**
     * Registers a parameterized preview whose render lambda receives the live [PreviewParamState].
     *
     * @param name Display name shown in the browser list.
     * @param group Group this preview belongs to.
     * @param params Param definitions built with [previewParams].
     * @param description Optional subtitle shown on the detail screen.
     * @param tags Searchable tags.
     * @param key Unique deduplication key. Defaults to `"${group.name}/$name"`.
     * @param composable Render lambda receiving the current [PreviewParamState].
     */
    fun preview(
        name: String,
        group: PreviewGroup,
        params: PreviewParamsDsl,
        description: String = "",
        tags: List<String> = emptyList(),
        key: String = "${group.name}/$name",
        composable: @Composable (PreviewParamState) -> Unit,
    ) {
        _entries += PreviewEntry(
            key = key,
            name = name,
            group = group,
            description = description,
            tags = tags,
            composable = {
                val state = LocalPreviewParamState.current
                composable(state)
            },
            paramForm = params.buildParamForm(),
            paramDefaults = params.buildDefaults(),
        )
    }

    internal fun build(): PreviewRegistry = object : PreviewRegistry {
        override val entries: List<PreviewEntry> = _entries.toList()
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
