package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Composable
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.AccessibilityPreviewState
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamState

/** Wraps the entire browser UI, typically to apply an app theme or surrounding chrome. */
typealias BrowserWrapper = @Composable (content: @Composable () -> Unit) -> Unit

/** Wraps the preview-list screen for a specific [PreviewGroup]. */
typealias GroupWrapper   = @Composable (group: PreviewGroup, content: @Composable () -> Unit) -> Unit

/** Wraps an individual preview render, for example to add padding or a device frame. */
typealias PreviewWrapper = @Composable (entry: PreviewEntry, content: @Composable () -> Unit) -> Unit

/** Renders and updates a preview's parameter controls. */
typealias PreviewParamForm = @Composable (
    state: PreviewParamState,
    onUpdate: (PreviewParamState) -> Unit,
) -> Unit

/**
 * Wraps an individual preview render with the currently resolved accessibility test-mode state.
 *
 * Use this to provide app-specific themes, semantics providers, or other accessibility
 * environment hooks that should react to the browser's accessibility settings.
 */
typealias AccessibilityWrapper = @Composable (
    state: AccessibilityPreviewState,
    entry: PreviewEntry,
    content: @Composable () -> Unit,
) -> Unit
