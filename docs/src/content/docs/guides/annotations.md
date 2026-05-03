---
title: Annotations
description: The low-friction path for discovered previews and basic parameters.
---

`@ComposePreview` is the entry point for generated previews. It declares the preview name, owning
group, optional description, tags, and optional variant family metadata.

## Groups and tags

Groups and tags are regular Kotlin hierarchies:

```kotlin
sealed interface MyGroup : PreviewGroup {
    data object Components : MyGroup {
        override val name = "Components"
    }
}

sealed interface MyTag : PreviewTag {
    data object Button : MyTag {
        override val title = "button"
    }
}
```

Identity comes from the type itself. `name` and `title` are only labels shown in the UI.

## Annotating a preview

```kotlin
@ComposePreview(
    name = "Primary Button",
    group = MyGroup.Components::class,
    description = "Default button style",
    tags = [MyTag.Button::class],
)
@Composable
fun PrimaryButtonPreview() {
    PrimaryButton(label = "Click me")
}
```

## What `@PreviewParam` is for

`@PreviewParam` is intentionally basic mode. It works well for common scalar parameters where the
annotation alone should describe the control.

```kotlin
@Composable
fun InboxSummaryPreview(
    @PreviewParam(label = "Filter") filter: InboxFilter = InboxFilter.All,
    @PreviewParam(label = "Notifications") notificationsEnabled: Boolean = true,
) {
    ...
}
```

Rules:

- The annotated parameter must have a Kotlin default value in the function signature.
- The function default is the single source of truth.
- `options` is available when you want a constrained string-backed picker.

When one preview needs a more expressive control surface, keep discovery via annotations and attach
a richer parameter form through a preview override instead of abandoning generated previews
entirely.
