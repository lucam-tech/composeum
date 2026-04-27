package tech.lucam.composeum.runtime.store

import kotlinx.coroutines.flow.Flow

/** Platform-independent persistence contract for [RuntimeSettings]. */
interface SettingsStorage {
    /** Live stream of the current persisted [RuntimeSettings]. */
    val settings: Flow<RuntimeSettings>

    /**
     * Applies [block] to the current [RuntimeSettings] and persists the result.
     * [block] receives the current settings as the receiver and must return the updated settings.
     */
    suspend fun update(block: RuntimeSettings.() -> RuntimeSettings)

    /** Resets all persisted settings to their platform defaults. */
    suspend fun reset()
}
