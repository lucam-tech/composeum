---
title: Parameters
description: Basic annotation-backed parameters, typed state, and richer DSL-backed forms.
---

Composeum supports two parameter levels:

- `@PreviewParam` for basic, annotation-driven controls
- DSL-built forms and preview overrides when you need richer controls for a specific preview

## Basic mode

`@PreviewParam` covers the common case:

```kotlin
@Composable
fun ProfileCardPreview(
    @PreviewParam(label = "Name") name: String = "Jane Doe",
    @PreviewParam(label = "Online") online: Boolean = true,
) {
    ...
}
```

This keeps preview discovery friction low and keeps defaults in normal Kotlin code.

## Typed runtime state

Manual and override-backed parameter forms use `PreviewParamKey<T>` and `PreviewParamState`:

```kotlin
val titleKey = previewParamKey<String>("title")

val state = PreviewParamState()
    .put(titleKey, "Composeum")

val title = state[titleKey] ?: "Fallback"
```

This avoids the older untyped map access pattern for custom preview surfaces.

## Richer controls for a single discovered preview

You can keep a preview annotation-driven and still override its parameter UI:

```kotlin
override val config = previewConfig {
    preview("tech.example.AppPreviews.Components/Primary Button") {
        params {
            val title = string(
                key = "title",
                label = "Label",
                initial = "Continue",
            )
            boolean(
                key = "enabled",
                label = "Enabled",
                initial = true,
            )
        }
    }
}
```

`params { ... }` builds both the parameter form and the initial typed defaults. If you need complete
control, use `paramForm { state, onUpdate -> ... }`.

## Custom parameter widgets

When the library does not know how to edit a type, register a custom field in config:

```kotlin
val config = previewConfig {
    customTypeField<StarRating>(initialValue = StarRating(3)) { value, onValue ->
        StarRatingWidget(value, onValue)
    }
}
```

The widget becomes the editor for matching parameter types, and the initial value is seeded before
first render.
