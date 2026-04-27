package tech.lucam.composeum.runtime.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.platform.LocalContext
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.store.DataStoreSettingsStorage

private val Context.previewSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "preview_settings",
)

/**
 * Android convenience overload of [ComposeumBrowser] that automatically creates a
 * [DataStoreSettingsStorage] backed by the app's DataStore.
 *
 * Use this overload from an Android `Activity.setContent` block or [ComposeumBrowserActivity].
 * On wasmJs, use the overload that takes an explicit `storage` parameter instead.
 *
 * @param registry The registry of all previews to browse.
 * @param config   Configuration for the browser. Build with [tech.lucam.composeum.runtime.config.previewConfig].
 * @param modifier Modifier applied to the root scaffold.
 */
@Composable
fun ComposeumBrowser(
    registry: PreviewRegistry,
    config: PreviewConfig = PreviewConfig(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val storage = remember(context) { DataStoreSettingsStorage(context.previewSettingsDataStore) }
    ComposeumBrowser(registry = registry, config = config, storage = storage, modifier = modifier)
}
