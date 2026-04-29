package tech.lucam.composeum.annotation

import kotlin.reflect.KClass

/**
 * Marks a function that returns an `android.view.View` as a preview to be displayed in the browser.
 *
 * The annotated function must satisfy these rules:
 * - Must NOT be `@Composable`.
 * - Must return `android.view.View` or a subtype.
 * - May have zero parameters, or exactly one parameter of type `android.content.Context`.
 *
 * The KSP processor wraps the function call in an `AndroidView {}` composable so that it
 * integrates seamlessly with the existing `PreviewEntry` model and rendering pipeline.
 *
 * Example — inflating an XML layout:
 * ```kotlin
 * @ViewPreview(name = "Profile Card (XML)", group = AppGroup.Legacy::class)
 * fun profileCardPreview(context: Context): View =
 *     LayoutInflater.from(context).inflate(R.layout.view_profile_card, null, false)
 * ```
 *
 * @param name        Human-readable display name shown in the browser.
 * @param group       A [PreviewGroup] object that determines where this preview appears in the tree.
 * @param description Optional longer description displayed below the preview name.
 * @param tags        Optional searchable tags used to filter previews.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewPreview(
    val name: String,
    val group: KClass<out PreviewGroup>,
    val description: String = "",
    val tags: Array<KClass<out PreviewTag>> = [],
)
