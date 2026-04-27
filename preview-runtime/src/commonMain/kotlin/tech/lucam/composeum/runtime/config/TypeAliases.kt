package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Composable
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry

typealias BrowserWrapper = @Composable (content: @Composable () -> Unit) -> Unit
typealias GroupWrapper   = @Composable (group: PreviewGroup, content: @Composable () -> Unit) -> Unit
typealias PreviewWrapper = @Composable (entry: PreviewEntry, content: @Composable () -> Unit) -> Unit
