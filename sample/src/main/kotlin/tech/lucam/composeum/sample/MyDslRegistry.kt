package tech.lucam.composeum.sample

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.runtime.buildRegistry
import tech.lucam.composeum.runtime.previewParams

/**
 * Previews registered by hand using [buildRegistry] — the DSL alternative to
 * the KSP [tech.lucam.composeum.annotation.ComposePreview] annotation.
 *
 * Use this pattern for composables you cannot (or prefer not to) annotate:
 * third-party components, composables that live in a different module, or
 * one-off test cases you want to keep out of production source.
 */
val MyDslRegistry = buildRegistry {

    // Simple preview — no interactive parameters.
    preview(
        name = "DSL Greeting",
        group = SampleGroup.Components,
        description = "A greeting card registered manually via buildRegistry.",
        tags = listOf("dsl", "text"),
    ) {
        Text(
            text = "Hello from the DSL registry!",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }

    // Parameterized preview — render lambda receives the live PreviewParamState.
    preview(
        name = "DSL Button",
        group = SampleGroup.Components,
        description = "A button whose label and enabled state are controlled from the param panel.",
        tags = listOf("dsl", "button"),
        params = previewParams {
            string(key = "label", default = "Press me", label = "Button label")
            boolean(key = "enabled", default = true, label = "Enabled")
        },
    ) { state ->
        Button(
            onClick = {},
            enabled = state["enabled"] ?: true,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(state["label"] ?: "Press me")
        }
    }

    // Parameterized preview using multiple param types.
    preview(
        name = "DSL Card",
        group = SampleGroup.Showcase,
        description = "A card with a configurable headline and body, registered via DSL.",
        tags = listOf("dsl", "card"),
        params = previewParams {
            string(key = "headline", default = "Card headline", label = "Headline")
            string(key = "body", default = "Card body text.", label = "Body")
            dropdown(
                key = "variant",
                options = listOf("Elevated", "Filled", "Outlined"),
                default = "Filled",
                label = "Variant",
            )
            color(key = "tint", default = Color.Unspecified, label = "Content tint")
        },
    ) { state ->
        val headline: String = state["headline"] ?: "Card headline"
        val body: String = state["body"] ?: "Card body text."
        val tint: Color = state["tint"] ?: Color.Unspecified
        val textColor = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint

        Card(modifier = Modifier.padding(16.dp)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}
