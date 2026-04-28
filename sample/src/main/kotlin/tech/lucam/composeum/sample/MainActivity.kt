package tech.lucam.composeum.sample

import tech.lucam.composeum.runtime.config.previewConfig
import tech.lucam.composeum.runtime.ui.ComposeumBrowserActivity
import tech.lucam.composeum.runtime.ui.LocalIsDarkTheme
import tech.lucam.composeum.runtime.ui.LocalPreviewTheme
import tech.lucam.composeum.sample.generated.GeneratedPreviewRegistry

/**
 * Minimal browser host for the starter sample.
 *
 * This is the recommended Android integration shape: subclass
 * [ComposeumBrowserActivity], provide the generated registry, and optionally
 * wrap the browser in your app theme.
 */
class MainActivity : ComposeumBrowserActivity() {

    override val registry = GeneratedPreviewRegistry

    override val config = previewConfig {
        browserWrapper { content ->
            val isDark = LocalIsDarkTheme.current
            val theme = LocalPreviewTheme.current
            SampleTheme(darkTheme = isDark, theme = theme) { content() }
        }
    }
}
