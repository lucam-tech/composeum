package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.compositionLocalOf
import tech.lucam.composeum.runtime.store.SettingsStorage

/**
 * Provides the [SettingsStorage] to descendant composables such as [SettingsSheet].
 * Always provided by [tech.lucam.composeum.runtime.ui.ComposeumBrowser].
 */
val LocalSettingsStorage = compositionLocalOf<SettingsStorage> {
    error("LocalSettingsStorage not provided — ensure ComposeumBrowser is an ancestor composable.")
}
