package tech.lucam.composeum.sample

import tech.lucam.composeum.runtime.ui.ComposeumBrowserActivity
import tech.lucam.composeum.sample.previews.generated.GeneratedPreviewRegistry

/**
 * Minimal browser host for the starter sample.
 *
 * This is the recommended zero-config Android integration shape: subclass
 * [ComposeumBrowserActivity] and provide the generated registry.
 */
class MainActivity : ComposeumBrowserActivity() {

    override val registry = GeneratedPreviewRegistry
}
