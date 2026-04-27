package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Composable

/**
 * A custom icon button added to the top app bar via [PreviewConfig.topBarActions].
 *
 * Use [PreviewConfigBuilder.topBarAction] to register instances rather than constructing directly.
 */
class TopBarAction(
    /** Accessibility label shown to screen readers. */
    val contentDescription: String,
    /** Composable that renders the icon, typically [androidx.compose.material3.Icon]. */
    val icon: @Composable () -> Unit,
    /** Invoked when the user taps this action. */
    val onClick: () -> Unit,
)
