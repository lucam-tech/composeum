package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.runtime.Composable
import tech.lucam.composeum.runtime.PreviewParamState
import tech.lucam.composeum.runtime.ui.component.LocalPreviewConfig

/**
 * Dispatches to a [tech.lucam.composeum.runtime.config.CustomParamField] widget registered
 * in [tech.lucam.composeum.runtime.config.PreviewConfig.customTypeFields] for [kotlinType].
 *
 * Falls back to [PreviewStringField] when no registration is found, storing the value as a
 * plain string. Emitted by the KSP-generated param form for parameters whose types are not
 * natively supported by the library.
 *
 * @param label       Label shown above or beside the widget.
 * @param kotlinType  Fully-qualified Kotlin type name used to look up the registered field.
 * @param stateKey    Key under which the value is stored in [state].
 * @param state       Current param state snapshot.
 * @param onUpdate    Called with the new state when the value changes.
 * @param description Optional description shown below the widget.
 */
@Composable
fun PreviewCustomTypeField(
    label: String,
    kotlinType: String,
    stateKey: String,
    state: PreviewParamState,
    onUpdate: (PreviewParamState) -> Unit,
    description: String = "",
) {
    val config = LocalPreviewConfig.current
    val field = config.customTypeFields[kotlinType]
    if (field != null) {
        val value = state.get<Any>(stateKey) ?: field.initialValue
        field.widget(value) { newValue -> onUpdate(state.put(stateKey, newValue)) }
    } else {
        PreviewStringField(
            label = label,
            value = state.get<String>(stateKey) ?: "",
            onValue = { onUpdate(state.put(stateKey, it)) },
            description = description,
        )
    }
}
