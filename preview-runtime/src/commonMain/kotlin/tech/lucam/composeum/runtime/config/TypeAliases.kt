package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Composable
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry

/** Wraps the entire browser UI, typically to apply an app theme or surrounding chrome. */
typealias BrowserWrapper = @Composable (content: @Composable () -> Unit) -> Unit

/** Wraps the preview-list screen for a specific [PreviewGroup]. */
typealias GroupWrapper   = @Composable (group: PreviewGroup, content: @Composable () -> Unit) -> Unit

/** Wraps an individual preview render, for example to add padding or a device frame. */
typealias PreviewWrapper = @Composable (entry: PreviewEntry, content: @Composable () -> Unit) -> Unit
