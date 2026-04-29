package tech.lucam.composeum.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Decomposed padding for a preview parameter.
 *
 * Unlike [PaddingValues], which is an opaque interface, this data class exposes each
 * side individually so that the param widget can render and update them as four independent
 * sliders. Call [toPaddingValues] to convert back to a standard [PaddingValues] for use
 * in your composable.
 */
data class PreviewPaddingValues(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val start: Dp = 0.dp,
    val end: Dp = 0.dp,
) {
    /** Converts to a standard [PaddingValues] for use in Compose layouts. */
    fun toPaddingValues(): PaddingValues =
        PaddingValues(top = top, bottom = bottom, start = start, end = end)
}
