package tech.lucam.composeum.runtime.ui.component

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import tech.lucam.composeum.runtime.store.RuntimeSettings
import tech.lucam.composeum.runtime.store.SettingsStorage

/**
 * Provides the [SettingsStorage] to descendant composables such as [SettingsSheet].
 * Always provided by [tech.lucam.composeum.runtime.ui.ComposeumBrowser].
 */
val LocalSettingsStorage = compositionLocalOf<SettingsStorage> {
    object : SettingsStorage {
        private val state = MutableStateFlow(RuntimeSettings())
        override val settings: Flow<RuntimeSettings> = state
        override suspend fun update(block: RuntimeSettings.() -> RuntimeSettings) {
            state.value = state.value.block()
        }

        override suspend fun reset() {
            state.value = RuntimeSettings()
        }
    }
}
