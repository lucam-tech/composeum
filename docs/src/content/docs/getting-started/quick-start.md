---
title: Quick Start
description: From one group and one preview to a running catalog activity.
---

## 1. Define a group

Groups are regular Kotlin types that implement `PreviewGroup`. Their type identity is the stable
key; `name` and `description` are presentation.

```kotlin
import tech.lucam.composeum.annotation.PreviewGroup

sealed interface AppPreviews : PreviewGroup {
    data object Components : AppPreviews {
        override val name = "Components"
    }
}
```

## 2. Add a preview

```kotlin
import androidx.compose.runtime.Composable
import tech.lucam.composeum.annotation.ComposePreview

@ComposePreview(
    name = "Primary Button",
    group = AppPreviews.Components::class,
)
@Composable
fun PrimaryButtonPreview() {
    PrimaryButton(label = "Click me")
}
```

## 3. Host the generated registry

```kotlin
import com.example.previews.generated.GeneratedPreviewRegistry
import tech.lucam.composeum.runtime.ui.ComposeumBrowserActivity

class PreviewCatalogActivity : ComposeumBrowserActivity() {
    override val registry = GeneratedPreviewRegistry
}
```

Register the activity in `AndroidManifest.xml`:

```xml

<activity android:name=".PreviewCatalogActivity" android:exported="true" />
```

## 4. Add your app theme later

Keep the first pass minimal. Once the browser runs, wrap it with your app theme:

```kotlin
import tech.lucam.composeum.runtime.config.previewConfig

class PreviewCatalogActivity : ComposeumBrowserActivity() {
    override val registry = GeneratedPreviewRegistry

    override val config = previewConfig {
        browserWrapper { content ->
            MyAppTheme { content() }
        }
    }
}
```

That is usually enough to get a usable internal preview catalog into a debug or tools build.
