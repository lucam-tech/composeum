package tech.lucam.composeum.runtime

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private const val STRING_FQN = "kotlin.String"
private const val BOOLEAN_FQN = "kotlin.Boolean"
private const val INT_FQN = "kotlin.Int"
private const val LONG_FQN = "kotlin.Long"
private const val FLOAT_FQN = "kotlin.Float"
private const val DOUBLE_FQN = "kotlin.Double"
private const val COLOR_FQN = "androidx.compose.ui.graphics.Color"
private const val DP_FQN = "androidx.compose.ui.unit.Dp"
private const val TEXT_UNIT_FQN = "androidx.compose.ui.unit.TextUnit"

internal fun PreviewParamState.toShareableMap(entry: PreviewEntry): Map<String, String> {
    val hints = entry.shareableParamTypeHints()
    val result = linkedMapOf<String, String>()
    for ((key, value) in values) {
        val hint = hints[key] ?: continue
        serializeShareableValue(hint, value)?.let { result[key] = it }
    }
    return result
}

internal fun Map<String, String>.toPreviewParamState(entry: PreviewEntry): PreviewParamState {
    if (isEmpty()) return entry.paramDefaults.toInitialState()
    var state = entry.paramDefaults.toInitialState()
    val hints = entry.shareableParamTypeHints()
    for ((key, raw) in this) {
        val hint = hints[key] ?: continue
        parseShareableValue(hint, raw)?.let { state = state.put(key, it) }
    }
    return state
}

private fun PreviewEntry.shareableParamTypeHints(): Map<String, String> {
    if (paramTypeHints.isNotEmpty()) return paramTypeHints
    return paramDefaults.defaults.mapNotNull { (key, value) ->
        inferTypeHint(value)?.let { key to it }
    }.toMap()
}

private fun inferTypeHint(value: Any): String? = when (value) {
    is String -> STRING_FQN
    is Boolean -> BOOLEAN_FQN
    is Int -> INT_FQN
    is Long -> LONG_FQN
    is Float -> FLOAT_FQN
    is Double -> DOUBLE_FQN
    is Color -> COLOR_FQN
    is Dp -> DP_FQN
    is TextUnit -> TEXT_UNIT_FQN
    else -> null
}

private fun serializeShareableValue(hint: String, value: Any): String? = when (hint) {
    STRING_FQN -> value as? String
    BOOLEAN_FQN, INT_FQN, LONG_FQN, FLOAT_FQN, DOUBLE_FQN -> value.toString()
    COLOR_FQN -> (value as? Color)?.value?.toString()
    DP_FQN -> (value as? Dp)?.value?.toString()
    TEXT_UNIT_FQN -> (value as? TextUnit)?.let {
        val unit = when {
            it.isSp -> "Sp"
            it.isEm -> "Em"
            else -> "Unspecified"
        }
        "${it.value}|$unit"
    }
    else -> value as? String
}

private fun parseShareableValue(hint: String, raw: String): Any? = when (hint) {
    STRING_FQN -> raw
    BOOLEAN_FQN -> raw.toBooleanStrictOrNull()
    INT_FQN -> raw.toIntOrNull()
    LONG_FQN -> raw.toLongOrNull()
    FLOAT_FQN -> raw.toFloatOrNull()
    DOUBLE_FQN -> raw.toDoubleOrNull()
    COLOR_FQN -> raw.toULongOrNull()?.let(::Color)
    DP_FQN -> raw.toFloatOrNull()?.let(::Dp)
    TEXT_UNIT_FQN -> raw.toTextUnitOrNull()
    else -> raw
}

private fun String.toTextUnitOrNull(): TextUnit? {
    val parts = split("|")
    if (parts.size != 2) return null
    val value = parts[0].toFloatOrNull() ?: return null
    return when (parts[1]) {
        "Sp" -> value.sp
        "Em" -> value.em
        else -> null
    }
}
