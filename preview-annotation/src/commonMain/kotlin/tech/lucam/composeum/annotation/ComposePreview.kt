package tech.lucam.composeum.annotation

import kotlin.reflect.KClass

/**
 * Marks a `@Composable` function as a preview to be collected and displayed in the preview browser.
 *
 * @param name        Human-readable display name shown in the browser.
 * @param group       A [PreviewGroup] object that determines where this preview appears in the tree.
 * @param description Optional longer description displayed below the preview name.
 * @param tags        Optional searchable tags used to filter previews.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ComposePreview(
    val name: String,
    val group: KClass<out PreviewGroup>,
    val description: String = "",
    val tags: Array<String> = [],
)
