---
title: Installation
description: Add Composeum to a module that declares previews and a host that renders the browser.
---

Composeum publishes three artifacts:

```text
tech.lucam.composeum:preview-annotation:0.2.0
tech.lucam.composeum:preview-runtime:0.2.0
tech.lucam.composeum:preview-ksp:0.2.0
```

Recommended split:

- Add `preview-annotation` to every module that declares previews.
- Add `preview-ksp` to those same modules with `ksp(...)`.
- Add `preview-runtime` only to the Android app or internal tools module that hosts the browser.

## Preview-declaring module

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("tech.lucam.composeum:preview-annotation:0.2.0")
    ksp("tech.lucam.composeum:preview-ksp:0.2.0")
}
```

No KSP arguments are required for the default path. Composeum generates `GeneratedPreviewRegistry`
under `<first preview package>.generated`.

## Browser host module

```kotlin
dependencies {
    implementation("tech.lucam.composeum:preview-runtime:0.2.0")
}
```

## Target support

- Stable and recommended: Android hosting via `ComposeumBrowserActivity`
- Stable: preview declarations from shared `commonMain` consumed by Android builds
- Experimental: wasm/browser hosting and opt-in Android Studio `@Preview` import
- Unsupported today: iOS and desktop-native browser hosts

Android-only APIs:

- `ComposeumBrowserActivity`
- Android `ComposeumBrowser(...)` overload
- `DataStoreSettingsStorage`
- `@ViewPreview`

Shared APIs:

- `@ComposePreview`
- `@PreviewParam`
- `PreviewGroup`
- `PreviewRegistry`
- Common `ComposeumBrowser(...)` overload
