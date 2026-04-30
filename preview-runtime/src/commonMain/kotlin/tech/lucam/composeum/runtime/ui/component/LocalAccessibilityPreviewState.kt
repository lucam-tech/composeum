package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.compositionLocalOf
import tech.lucam.composeum.runtime.AccessibilityPreviewState

/**
 * Provides the resolved accessibility test-mode state to previews and wrappers.
 */
val LocalAccessibilityPreviewState = compositionLocalOf { AccessibilityPreviewState() }
