package tech.lucam.composeum.sample

import tech.lucam.composeum.annotation.PreviewGroup

/**
 * Small group tree for the starter sample.
 */
sealed interface SampleGroup : PreviewGroup {

    data object Components : SampleGroup {
        override val name: String = "Components"
        override val description: String = "Reusable components with small parameter surfaces."
    }

    sealed interface Screens : SampleGroup {
        data object Home : Screens {
            override val name: String = "Home"
            override val description: String = "Small screen-level previews."
        }
    }
}
