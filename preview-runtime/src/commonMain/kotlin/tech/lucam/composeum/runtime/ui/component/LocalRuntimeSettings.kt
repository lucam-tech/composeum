package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.compositionLocalOf
import tech.lucam.composeum.runtime.store.RuntimeSettings

/** Composition local exposing the raw persisted browser settings. */
val LocalRuntimeSettings = compositionLocalOf { RuntimeSettings() }
