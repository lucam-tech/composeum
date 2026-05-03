package tech.lucam.composeum.annotation

/**
 * Marks a composable function parameter as a live-editable preview parameter.
 *
 * The annotated parameter **must** have a default value in the function signature.
 *
 * @param label       Human-readable label shown in the param panel widget.
 * @param description Optional description shown below the widget.
 * @param options     If non-empty, renders a dropdown restricted to these string values.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class PreviewParam(
    val label: String,
    val description: String = "",
    val options: Array<String> = [],
)
