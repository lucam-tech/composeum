---
title: Configuration
description: Global config, explicit overrides, and per-preview customization.
---

Composeum separates resolved config from override intent:

- `previewConfig { ... }` builds a full `PreviewConfig`
- `previewConfigOverride { ... }` expresses mergeable overrides

That split matters when multiple registries contribute behavior.

## Global config

```kotlin
val config = previewConfig {
    fontScale = 1.1f
    uiScale = 0.95f
    showDescriptions = true
    showTags = true
    thumbnailColumns = 3
    groupExpansionMode = GroupExpansionMode.INLINE
}
```

## Per-group overrides

```kotlin
val config = previewConfig {
    groups {
        group(SampleGroup.Components::class) {
            thumbnailColumns = 2
        }
    }
}
```

## Per-preview overrides

```kotlin
val config = previewConfig {
    preview("tech.example.SampleGroup.Components/Profile Card") {
        showParamPanel = true
        params {
            string(key = "name", label = "Name", initial = "Jane Doe")
            boolean(key = "online", label = "Online", initial = true)
        }
    }
}
```

Useful per-preview knobs:

- `previewWrapper`
- `showParamPanel`
- `paramForm`
- `params { ... }`

## Registry-local overrides

If you are building a registry manually, use registry-local config and include semantics:

```kotlin
val registry = buildRegistry {
    config {
        showDescriptions = false
    }
}
```

That override stays attached to the registry and composes when another registry includes it.
