package tech.lucam.composeum.runtime.store

import tech.lucam.composeum.runtime.config.PreviewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Merges [PreviewConfig] (compile-time defaults) with the live [RuntimeSettings]
 * from [SettingsStorage] and exposes the result as [resolvedSettings].
 *
 * `isDark` in [resolvedSettings] is computed correctly for all three [ThemeOverride] values
 * when [systemIsDarkFlow] is provided. Without it the SYSTEM override always resolves to
 * the `config.isDarkMode` fallback.
 *
 * @param storage          Platform-specific settings persistence.
 * @param config           Compile-time browser configuration used as the baseline for defaults.
 * @param scope            Coroutine scope that owns the [resolvedSettings] state flow; should be
 *                         tied to the composable or host lifecycle that owns this instance.
 * @param systemIsDarkFlow Optional flow of the current device dark-mode state. Provide this by
 *                         converting `isSystemInDarkTheme()` via `snapshotFlow { isSystemInDarkTheme() }`.
 *                         When null, [ThemeOverride.SYSTEM] falls back to `config.isDarkMode ?: false`.
 */
class SettingsViewModel(
    private val storage: SettingsStorage,
    private val config: PreviewConfig,
    scope: CoroutineScope,
    systemIsDarkFlow: kotlinx.coroutines.flow.Flow<Boolean>? = null,
) {
    /** The fully-resolved settings, updated whenever storage or the system dark-mode state changes. */
    val resolvedSettings: StateFlow<ResolvedSettings> = if (systemIsDarkFlow != null) {
        combine(storage.settings, systemIsDarkFlow) { settings, systemIsDark ->
            settings.resolve(config, systemIsDark)
        }
    } else {
        storage.settings.map { it.resolve(config) }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = RuntimeSettings().resolve(config),
    )
}
