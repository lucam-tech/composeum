---
title: Manual Registry
description: Build previews explicitly when you need full control or want to compose registries.
---

Generated previews are the normal path, but the manual DSL is available when you need:

- previews not driven by annotations
- custom parameter surfaces
- composed registries across modules
- local registry-specific configuration overrides

## Building a registry

```kotlin
val registry = buildRegistry {
    group(SampleGroup.Components) {
        preview("Greeting Card", group = SampleGroup.Components) {
            GreetingPreview()
        }

        preview(
            name = "Profile Card",
            group = SampleGroup.Components,
            params = previewParams {
                string(key = "name", label = "Name", initial = "Jane Doe")
                boolean(key = "online", label = "Online", initial = true)
            },
        ) { state ->
            ProfileCard(
                name = state["name"] ?: "Jane Doe",
                online = state["online"] ?: true,
            )
        }
    }
}
```

## Composing registries

```kotlin
val appRegistry = buildRegistry {
    include(FeaturePreviews)
    include(DesignSystemPreviews)
}
```

Included registries bring both entries and config overrides.

## Why keep this path

The DSL is where Composeum can expose richer control surfaces without making the annotation model
heavy. Use it when the preview surface itself is part of the tool you are building.
