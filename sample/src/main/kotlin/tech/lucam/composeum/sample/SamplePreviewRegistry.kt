package tech.lucam.composeum.sample

import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.buildRegistry
import tech.lucam.composeum.runtime.previewParams
import tech.lucam.composeum.sample.previews.GreetingPreview
import tech.lucam.composeum.sample.previews.InboxSummaryPreview
import tech.lucam.composeum.sample.previews.InboxFilter
import tech.lucam.composeum.sample.previews.PrimaryButtonPreview
import tech.lucam.composeum.sample.previews.ProfileCardPreview

internal val SamplePreviewRegistry: PreviewRegistry = buildRegistry {
    preview(
        name = "Greeting Card",
        group = SampleGroup.Components,
        description = "A simple preview with no parameters.",
        key = "tech.lucam.composeum.sample.previews.GreetingPreview",
    ) {
        GreetingPreview()
    }

    preview(
        name = "Primary Button",
        group = SampleGroup.Components,
        params = previewParams {
            string(
                key = "label",
                default = "Continue",
                label = "Label",
            )
        },
        description = "A button with one string-backed preview parameter.",
        tags = listOf("button", "cta"),
        key = "tech.lucam.composeum.sample.previews.PrimaryButtonPreview",
    ) { state ->
        PrimaryButtonPreview(
            label = state["label"] ?: "Continue",
        )
    }

    preview(
        name = "Inbox Summary",
        group = SampleGroup.Screens.Home,
        params = previewParams {
            dropdown(
                key = "filter",
                options = InboxFilter.entries.map { it.name },
                default = InboxFilter.All.name,
                label = "Filter",
            )
            boolean(
                key = "notificationsEnabled",
                default = true,
                label = "Notifications",
            )
        },
        description = "A small screen preview with enum and boolean parameters.",
        key = "tech.lucam.composeum.sample.previews.InboxSummaryPreview",
    ) { state ->
        InboxSummaryPreview(
            filter = InboxFilter.valueOf(state["filter"] ?: InboxFilter.All.name),
            notificationsEnabled = state["notificationsEnabled"] ?: true,
        )
    }

    preview(
        name = "Profile Card",
        group = SampleGroup.Components,
        params = previewParams {
            string(
                key = "name",
                default = "Jane Doe",
                label = "Name",
            )
            boolean(
                key = "online",
                default = true,
                label = "Online",
            )
        },
        description = "A slightly richer component preview with two basic parameters.",
        tags = listOf("card", "profile"),
        key = "tech.lucam.composeum.sample.previews.ProfileCardPreview",
    ) { state ->
        ProfileCardPreview(
            name = state["name"] ?: "Jane Doe",
            online = state["online"] ?: true,
        )
    }
}
