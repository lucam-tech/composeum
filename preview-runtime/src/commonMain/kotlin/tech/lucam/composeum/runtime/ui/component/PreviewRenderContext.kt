package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Context injected into every preview composable by the Composeum browser.
 *
 * Use [LocalPreviewRenderContext] to read the current value inside a `@ComposePreview`-
 * annotated function or a DSL-registered composable:
 *
 * ```kotlin
 * val ctx = LocalPreviewRenderContext.current
 * LaunchedEffect(ctx.isThumbnail) {
 *     if (!ctx.isThumbnail) focusRequester.requestFocus()
 * }
 * ```
 *
 * @param isThumbnail `true` when the preview is rendering inside a small thumbnail card
 *   in the list view; `false` when rendering on the full-size detail screen.
 */
data class PreviewRenderContext(
    val isThumbnail: Boolean,
)

/**
 * Provides the active [PreviewRenderContext] to the composition tree.
 *
 * The browser sets this to `isThumbnail = true` inside [PreviewThumbnailCard] and
 * `isThumbnail = false` inside [tech.lucam.composeum.runtime.ui.screen.PreviewDetailScreen].
 * The default (`isThumbnail = false`) is correct for any composable rendered outside the
 * browser (e.g. in a standalone app or a standard Compose preview tool).
 */
val LocalPreviewRenderContext = staticCompositionLocalOf { PreviewRenderContext(isThumbnail = false) }
