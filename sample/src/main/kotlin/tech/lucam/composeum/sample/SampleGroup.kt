package tech.lucam.composeum.sample

import tech.lucam.composeum.annotation.PreviewGroup

/**
 * Top-level sealed group hierarchy for the sample app's preview browser.
 *
 * The hierarchy intentionally uses both flat leaf groups (Components, Screens,
 * Forms, Showcase) and a nested sealed interface (Themed) to demonstrate the
 * library's support for arbitrarily deep sealed group trees.
 */
sealed interface SampleGroup : PreviewGroup {

    /** Previews for individual UI components (buttons, cards, inputs, etc.). */
    data object Components : SampleGroup {
        override val name: String = "Components"
        override val description: String = "Reusable UI components such as buttons, cards, and inputs."
    }

    /** Previews for full-screen layouts and navigation destinations. */
    data object Screens : SampleGroup {
        override val name: String = "Screens"
        override val description: String = "Full-screen layouts and navigation destinations."
    }

    /** Previews for form controls demonstrating all @PreviewParam types. */
    data object Forms : SampleGroup {
        override val name: String = "Forms"
        override val description: String = "Input controls covering Float, Int, String, and options-dropdown params."
    }

    /**
     * Nested sealed group demonstrating a multi-level hierarchy.
     *
     * The browser renders "Themed" as a collapsible parent node. Its children
     * (Dark, Accented) are the navigable leaf groups, each with its own
     * per-group config override in [MainActivity].
     */
    sealed interface Themed : SampleGroup {

        /** Previews rendered against a dark-surface group background. */
        data object Dark : Themed {
            override val name: String = "Dark"
            override val description: String = "Components previewed on a dark surface — useful for dark-mode spot-checks."
        }

        /** Previews rendered against a primary-container group background. */
        data object Accented : Themed {
            override val name: String = "Accented"
            override val description: String = "Components on a primary-container background to test brand-colour contrast."
        }
    }

    /**
     * Wide, single-column previews demonstrating the per-group
     * [thumbnailColumns] override and a bordered [previewWrapper].
     */
    data object Showcase : SampleGroup {
        override val name: String = "Showcase"
        override val description: String = "Full-width previews with thumbnailColumns = 1 and a custom bordered wrapper."
    }

    /** Previews for View-based XML layouts rendered via AndroidView. */
    data object XmlLayouts : SampleGroup {
        override val name: String = "XML Layouts"
        override val description: String = "Legacy View-based XML layouts previewed alongside Composables."
    }

    /**
     * Previews that exercise every @PreviewParam type: enum, nullable, data class,
     * sealed interface, List<T>, and custom types with registered widgets.
     */
    data object ParamTypes : SampleGroup {
        override val name: String = "Param Types"
        override val description: String =
            "One preview per @PreviewParam type — enum, nullable, data class, " +
            "sealed interface, List<T>, and custom type with a registered widget."
    }
}
