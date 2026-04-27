package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Composable

/**
 * A custom composable widget and default value for a `@PreviewParam`-annotated parameter
 * whose type the library does not natively support.
 *
 * Register instances in [PreviewConfig.customTypeFields], keyed by the fully-qualified Kotlin
 * type name (e.g. `"com.example.MyType"`).
 *
 * Prefer the type-safe [customParamField] helper over constructing this directly.
 */
data class CustomParamField(
    /**
     * The value placed in state when the param has no persisted value yet.
     * Must be an instance of the type this field handles.
     */
    val initialValue: Any,
    /**
     * Composable that renders the editor widget.
     * Receives the current value (cast to `Any`; use [customParamField] for full type safety)
     * and emits updated values via [onValue].
     */
    val widget: @Composable (value: Any, onValue: (Any) -> Unit) -> Unit,
)

/**
 * Creates a [CustomParamField] for type [T] with full type safety inside [widget].
 *
 * Example:
 * ```kotlin
 * customParamField<StarRating>(
 *     initialValue = StarRating(3),
 * ) { value, onValue ->
 *     StarRatingWidget(stars = value.stars, onSelect = { onValue(StarRating(it)) })
 * }
 * ```
 *
 * @param initialValue The default instance used when no persisted value exists in state.
 * @param widget The composable widget receiving the current [T] value and emitting new ones.
 */
inline fun <reified T : Any> customParamField(
    initialValue: T,
    noinline widget: @Composable (value: T, onValue: (T) -> Unit) -> Unit,
): CustomParamField = CustomParamField(
    initialValue = initialValue,
    widget = { v, ov ->
        @Suppress("UNCHECKED_CAST")
        widget(v as T) { ov(it) }
    },
)
