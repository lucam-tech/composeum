---
title: Browser Hosting
description: Android-first hosting, wrappers, and source links.
---

## Android activity host

The canonical host is `ComposeumBrowserActivity`:

```kotlin
class PreviewCatalogActivity : ComposeumBrowserActivity() {
    override val registry = GeneratedPreviewRegistry
}
```

This is the most straightforward path for internal tooling, QA builds, and design review builds.

## Wrapping the browser

Most teams want the browser rendered inside the app theme:

```kotlin
override val config = previewConfig {
    browserWrapper { content ->
        MyAppTheme { content() }
    }
}
```

You can also apply wrappers at narrower scopes:

- `groupWrapper`
- `previewWrapper`
- `accessibilityWrapper`

## Source links

The detail screen can link to repository source instead of only trying `idea://open`:

```kotlin
override val config = previewConfig {
    sourceBaseUrl = "https://github.com/your-org/your-repo/blob/main"
    sourceStripPrefix = "/absolute/path/to/repo/root"
}
```

That makes preview detail screens useful outside the local Android Studio workflow.

## Runtime controls

The browser config also controls:

- locale
- theme options
- font scale
- UI scale
- group expansion mode
- settings sheet composition
- top bar actions

Treat the browser host as product tooling, not just as a debug screen shell.
