# 08 — Target Support And Adoption Guidance

This document is the consumer-facing summary of what Composeum supports today,
which entry points to use on each target, and which parts of the product are
stable versus still evolving.

Use this alongside the README:

- the README is the install guide
- this document is the support and adoption guide
- `03-tech-spec.md` is the implementation-level design reference

---

## Support Matrix

| Use case | Status | What to use |
| --- | --- | --- |
| Android app hosts the preview browser in a debug/internal activity | Stable and recommended | `ComposeumBrowserActivity` from `:preview-runtime` |
| Android app hosts the preview browser manually inside its own `Activity.setContent` | Stable | Android `ComposeumBrowser(...)` overload from `:preview-runtime` |
| Shared preview declarations in `commonMain` consumed by Android builds | Stable | `@ComposePreview` + KSP in the consuming target |
| Shared preview declarations in `commonMain` consumed by a wasm/browser build | Experimental | Common `ComposeumBrowser(...)` + `LocalStorageSettingsStorage()` |
| Android `View` / XML previews via `@ViewPreview` | Stable on Android only | `@ViewPreview` in Android source sets |
| Automatic import of Android Studio `@Preview` annotations | Experimental and opt-in | `composeum.includeAndroidPreview=true` |
| iOS preview browser host | Unsupported | No shipped host today |
| Native desktop preview browser host | Unsupported | No shipped host today |

---

## Recommended Adoption Paths

### Android-first teams

This is the default path and the one the README optimizes for.

Use:

1. `preview-annotation` in modules that declare previews
2. `preview-ksp` via `ksp(...)` in those same modules
3. `preview-runtime` only in the Android app/debug host module
4. A small subclass of `ComposeumBrowserActivity`

This path gives you the least setup friction and the most complete runtime
support in the current release.

### Kotlin Multiplatform teams with shared UI

Shared preview declarations in `commonMain` are supported, but the browser host
is still platform-specific:

- Android can use `ComposeumBrowserActivity` or the Android `ComposeumBrowser(...)` overload
- wasm/browser builds must call the common `ComposeumBrowser(...)` directly and provide storage

Adopt this path when your previews already live in shared Compose UI code and
you are comfortable owning a small amount of platform-specific browser wiring.

### Teams evaluating Web/wasm hosting

Treat the wasm/browser runtime as experimental.

The core registry and browser composable exist, but the Android host
convenience layer does not apply there, and the project does not yet position
wasm hosting as the primary first-time adoption path.

---

## Entry Points By Target

### Android-only entry points

These APIs are Android-specific and should be used only from Android source
sets:

- `ComposeumBrowserActivity`
- Android `ComposeumBrowser(registry, config, modifier)` overload
- `DataStoreSettingsStorage`
- `@ViewPreview`

Use these when the browser is hosted inside an Android app.

### Shared/common entry points

These APIs are target-agnostic and can be used from shared Compose code:

- `@ComposePreview`
- `@PreviewParam`
- `PreviewGroup`
- `PreviewRegistry` and generated registries
- Common `ComposeumBrowser(registry, config, storage, modifier)` overload

Use these when you want preview declarations and browser UI code to stay in
shared modules.

---

## Stable Vs Experimental

### Stable today

- Android preview browser hosting
- Compile-time discovery of `@ComposePreview`
- Generated registries consumed through `PreviewRegistry`
- Basic interactive parameter controls on supported types
- Android `@ViewPreview`

### Experimental today

- wasm/browser hosting workflow
- Automatic import of Android Studio `@Preview`
- Broader multiplatform support beyond Android and wasm/browser

### Unsupported today

- iOS browser hosting
- Desktop-native host integrations
- Treating generated registry internals as public API

---

## Practical Guidance

### If you want the shortest path to first browser launch

Follow the README starter sample and host the browser on Android.

### If you want one shared preview catalog across targets

Keep preview declarations in shared source sets, but assume the host layer and
settings storage remain target-specific.

### If you depend on Android Studio `@Preview`

Enable the opt-in processor flag intentionally and treat it as compatibility
support, not the primary Composeum authoring model.

---

## Related Docs

- [README](../../README.md)
- [03-tech-spec.md](./03-tech-spec.md)
- [07-roadmap-tasks.md](./07-roadmap-tasks.md)
