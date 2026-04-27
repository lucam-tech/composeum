package tech.lucam.composeum.runtime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.PreviewConfig

/**
 * A ready-to-use Activity that hosts [ComposeumBrowser].
 * Subclass this and override [registry] and optionally [config].
 *
 * Add the subclass to your debug `AndroidManifest.xml` with `android:exported="true"`.
 *
 * Example:
 * ```kotlin
 * class CatalogActivity : ComposeumBrowserActivity() {
 *     override val registry = GeneratedPreviewRegistry
 *     override val config = previewConfig { browserWrapper { content -> MyTheme { content() } } }
 * }
 * ```
 */
open class ComposeumBrowserActivity : ComponentActivity() {

    /** Override to provide the preview registry. */
    open val registry: PreviewRegistry
        get() = error("Override ComposeumBrowserActivity.registry")

    /** Override to provide a custom config. Defaults to [PreviewConfig] defaults. */
    open val config: PreviewConfig
        get() = PreviewConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeumBrowser(registry = registry, config = config)
        }
    }
}
