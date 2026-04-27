package tech.lucam.composeum.runtime.store

import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * wasmJs implementation of [SettingsStorage] backed by [window.localStorage].
 * Settings survive page reloads but not cross-origin storage clears.
 */
class LocalStorageSettingsStorage : SettingsStorage {

    private val _settings = MutableStateFlow(loadFromStorage())

    override val settings: Flow<RuntimeSettings> = _settings

    override suspend fun update(block: RuntimeSettings.() -> RuntimeSettings) {
        val next = _settings.value.block()
        _settings.value = next
        saveToStorage(next)
    }

    override suspend fun reset() {
        window.localStorage.removeItem(KEY)
        _settings.value = RuntimeSettings()
    }

    companion object {
        private const val KEY = "compose_preview_runtime_settings"
        private val json = Json { ignoreUnknownKeys = true }

        private fun loadFromStorage(): RuntimeSettings {
            val raw = window.localStorage.getItem(KEY) ?: return RuntimeSettings()
            return runCatching { json.decodeFromString<RuntimeSettings>(raw) }
                .getOrDefault(RuntimeSettings())
        }

        private fun saveToStorage(s: RuntimeSettings) {
            window.localStorage.setItem(KEY, json.encodeToString(s))
        }
    }
}
