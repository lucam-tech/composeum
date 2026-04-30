package tech.lucam.composeum.runtime

import kotlinx.serialization.Serializable

/** Supported color-vision simulation modes for accessibility testing. */
@Serializable
enum class ColorBlindMode {
    NONE,
    PROTANOPIA,
    DEUTERANOPIA,
    TRITANOPIA,
}

/**
 * Accessibility-related preview environment toggles.
 *
 * These are exposed to previews through a composition local so host apps can render
 * accessibility-oriented variants from inside their own themes, providers, and UI code.
 */
@Serializable
data class AccessibilityPreviewState(
    val screenReaderMode: Boolean = false,
    val highContrastMode: Boolean = false,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
    val reducedMotionMode: Boolean = false,
    val largeTouchTargetsMode: Boolean = false,
)
