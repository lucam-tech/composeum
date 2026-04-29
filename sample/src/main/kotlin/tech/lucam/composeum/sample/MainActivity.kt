package tech.lucam.composeum.sample

import tech.lucam.composeum.runtime.ui.ComposeumBrowserActivity

/**
 * Minimal browser host for the starter sample.
 *
 * Subclass [ComposeumBrowserActivity] and provide the sample registry.
 */
class MainActivity : ComposeumBrowserActivity() {

    override val registry = SamplePreviewRegistry
}
