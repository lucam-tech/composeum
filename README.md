# Composeum

**A compile-time Jetpack Compose preview browser for Android.**

Annotate your composables with `@ComposePreview`, run KSP, and get a fully interactive browser with live parameter controls — no reflection, no manual registration, no setup beyond a few lines.

---

## Contents

- [What it does](#what-it-does)
- [Screenshots](#screenshots)
- [Installation](#installation)
- [5-minute setup](#5-minute-setup)
- [Quick start](#quick-start)
- [Troubleshooting](#troubleshooting)
- [Annotation reference](#annotation-reference)
- [Defining preview groups](#defining-preview-groups)
- [KSP configuration](#ksp-configuration)
- [Launching the browser](#launching-the-browser)
- [Parameter controls](#parameter-controls)
- [Advanced: hand-built registry](#advanced-hand-built-registry)
- [Advanced: custom type fields](#advanced-custom-type-fields)
- [Advanced: config DSL reference](#advanced-config-dsl-reference)
- [Advanced: per-group overrides](#advanced-per-group-overrides)
- [Advanced: wrapping the browser](#advanced-wrapping-the-browser)
- [Advanced: source code links](#advanced-source-code-links)
- [Multi-module projects](#multi-module-projects)
- [Error isolation](#error-isolation)
- [Known limitations](#known-limitations)
- [Module architecture](#module-architecture)
- [Building locally](#building-locally)

---

## What it does

Composeum scans your source files at compile time (via KSP) and builds a typed registry of every composable you annotate with `@ComposePreview`. At runtime it renders a navigation-based browser where you can:

- Browse and search all previews, organized by your own group hierarchy
- Tweak every `@PreviewParam` parameter live with built-in widgets (string, boolean, int, float, colour, dp, sp, dropdown)
- Toggle dark/light mode, font scale, UI scale, and locale without restarting
- Deep-link directly to any preview
- See where each composable lives in source

No reflection is used. Every composable is discovered and registered at compile time.

---

## Screenshots

> _Add screenshots here once the sample app is run._

---

## Installation

Composeum publishes three artifacts:

```
tech.lucam.composeum:preview-annotation:0.1.0
tech.lucam.composeum:preview-runtime:0.1.0
tech.lucam.composeum:preview-ksp:0.1.0
```

> Current version: `0.1.0`

Recommended usage:

- Add `preview-annotation` to every module that declares previews
- Add `preview-ksp` to those same modules via `ksp(...)`
- Add `preview-runtime` only to the Android app/debug module that hosts the browser

Current support:

- Android browser hosting via `ComposeumBrowserActivity`
- Shared preview declarations from `commonMain`
- Wasm/Web runtime support in `preview-runtime`

The fastest setup path below is Android-first because that is the most direct
consumer flow today.

---

## 5-minute setup

### 1. Configure the module that contains previews

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("tech.lucam.composeum:preview-annotation:0.1.0")
    ksp("tech.lucam.composeum:preview-ksp:0.1.0")
}

ksp {
    // Optional: defaults to "<first preview package>.generated"
    arg("composeum.registryPackage", "com.example.previews.generated")

    // Optional: defaults to "GeneratedPreviewRegistry"
    arg("composeum.registryName", "GeneratedPreviewRegistry")
}
```

### 2. Add the runtime to the browser app module

```kotlin
dependencies {
    implementation("tech.lucam.composeum:preview-runtime:0.1.0")
}
```

### 3. Add one group and one preview

```kotlin
import androidx.compose.runtime.Composable
import tech.lucam.composeum.annotation.ComposePreview
import tech.lucam.composeum.annotation.PreviewGroup

sealed interface AppPreviews : PreviewGroup {
    data object Components : AppPreviews {
        override val name = "Components"
    }
}

@ComposePreview(name = "Primary Button", group = AppPreviews.Components::class)
@Composable
fun PrimaryButtonPreview() {
    PrimaryButton(label = "Click me")
}
```

### 4. Host the generated registry in a debug activity

```kotlin
import com.example.previews.generated.GeneratedPreviewRegistry
import tech.lucam.composeum.runtime.config.previewConfig
import tech.lucam.composeum.runtime.ui.ComposeumBrowserActivity

class PreviewCatalogActivity : ComposeumBrowserActivity() {
    override val registry = GeneratedPreviewRegistry

    override val config = previewConfig {
        browserWrapper { content ->
            MyAppTheme { content() }
        }
    }
}
```

### 5. Register the activity

```xml
<activity
    android:name=".PreviewCatalogActivity"
    android:exported="true" />
```

Build and launch the activity. Composeum discovers annotated previews during
KSP and renders them in the browser at runtime.

---

## Quick start

### 1. Define a group

Groups are plain sealed interfaces that implement `PreviewGroup`. You own the hierarchy — nest them as deeply as you want.

```kotlin
import tech.lucam.composeum.annotation.PreviewGroup

sealed interface MyGroup : PreviewGroup {
    data object Components : MyGroup {
        override val name = "Components"
    }
    data object Screens : MyGroup {
        override val name = "Screens"
        override val description = "Full-screen composables"
    }
}
```

### 2. Annotate your composables

```kotlin
import tech.lucam.composeum.annotation.ComposePreview

@ComposePreview(
    name = "Primary Button",
    group = MyGroup.Components::class,
    description = "Default button style",
    tags = ["button", "cta"],
)
@Composable
fun PrimaryButtonPreview() {
    PrimaryButton(label = "Click me")
}
```

### 3. Launch the browser

The canonical Android path is to subclass `ComposeumBrowserActivity` in a
debug-only or internal tools module:

```kotlin
import com.example.previews.generated.GeneratedPreviewRegistry
import tech.lucam.composeum.runtime.config.previewConfig
import tech.lucam.composeum.runtime.ui.ComposeumBrowserActivity

class PreviewCatalogActivity : ComposeumBrowserActivity() {

    override val registry = GeneratedPreviewRegistry

    override val config = previewConfig {
        browserWrapper { content ->
            MyAppTheme { content() }
        }
    }
}
```

Declare it in `AndroidManifest.xml`:

```xml
<activity
    android:name=".PreviewCatalogActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Build and run. The browser opens with all discovered previews.

If you do not want it in the launcher, omit the `intent-filter` and open the
activity from your debug menu or internal navigation.

### 4. Multi-module setup

Run KSP in every module that declares previews, then merge the generated
registries in the browser app:

```kotlin
// :feature-auth/build.gradle.kts
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("tech.lucam.composeum:preview-annotation:0.1.0")
    ksp("tech.lucam.composeum:preview-ksp:0.1.0")
}

ksp {
    arg("composeum.registryPackage", "com.example.auth.generated")
    arg("composeum.registryName", "AuthPreviewRegistry")
}
```

```kotlin
// :preview-app/src/main/kotlin/.../PreviewCatalogActivity.kt
import com.example.auth.generated.AuthPreviewRegistry
import com.example.feed.generated.FeedPreviewRegistry
import tech.lucam.composeum.runtime.CompositePreviewRegistry

class PreviewCatalogActivity : ComposeumBrowserActivity() {
    override val registry = CompositePreviewRegistry(
        AuthPreviewRegistry,
        FeedPreviewRegistry,
    )
}
```

Each module's generated registry is independent. `CompositePreviewRegistry`
merges them at runtime and keeps the first entry when keys overlap.

---

## Troubleshooting

### `GeneratedPreviewRegistry` cannot be resolved

Check all of the following:

- The module with previews applies `com.google.devtools.ksp`
- That same module depends on `preview-annotation` and `ksp(preview-ksp)`
- Your preview functions are annotated with `@ComposePreview`
- You imported the generated package that matches your KSP configuration

If you do not set `composeum.registryPackage`, the default package is:
`<first preview function package>.generated`.

### A preview does not appear in the browser

Common causes:

- The function is not `@Composable`
- The function is not annotated with `@ComposePreview`
- A `@PreviewParam` parameter is missing a Kotlin default value
- The preview lives in a module where KSP is not configured

### Browser activity compiles but renders unthemed UI

Wrap the browser in your app theme:

```kotlin
override val config = previewConfig {
    browserWrapper { content ->
        MyAppTheme { content() }
    }
}
```

### Android Studio `@Preview` annotations are not imported automatically

Composeum only processes Android Studio `@Preview` annotations when:

```kotlin
ksp {
    arg("composeum.includeAndroidPreview", "true")
}
```

Without that flag, only `@ComposePreview` and `@ViewPreview` are processed.

---

## Annotation reference

### `@ComposePreview`

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ComposePreview(
    val name: String = "",
    val group: KClass<out PreviewGroup> = PreviewGroup::class,
    val description: String = "",
    val tags: Array<String> = [],
)
```

| Parameter     | Required | Description                                                |
| ------------- | -------- | ---------------------------------------------------------- |
| `name`        | no       | Display name shown in the browser; defaults to the function name |
| `group`       | no       | The `PreviewGroup` leaf this preview belongs to; omit it to place the preview at the browser top level |
| `description` | no       | Shown below the preview name when descriptions are enabled |
| `tags`        | no       | Searchable labels                                          |

The annotated function must be `@Composable` and must be callable with no arguments (all parameters either have defaults or are annotated with `@PreviewParam`).

### `@PreviewParam`

Marks a composable parameter as controllable from the browser's parameter panel.

```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class PreviewParam(
    val label: String,
    val default: String = "",
    val description: String = "",
    val options: Array<String> = [],
)
```

| Parameter     | Description                                                     |
| ------------- | --------------------------------------------------------------- |
| `label`       | Widget label shown in the param panel                           |
| `default`     | String-encoded default value (e.g. `"true"`, `"16"`, `"Hello"`) |
| `description` | Tooltip / helper text for the widget                            |
| `options`     | Non-empty list turns the widget into a dropdown picker          |

**Every `@PreviewParam` parameter must have a Kotlin default value** in the function signature. This ensures the composable is callable from the browser with no initial state, and the KSP processor enforces it at compile time.

```kotlin
@ComposePreview(name = "Alert Banner", group = MyGroup.Components::class)
@Composable
fun AlertBannerPreview(
    @PreviewParam(label = "Message", default = "Something went wrong")
    message: String = "Something went wrong",

    @PreviewParam(label = "Dismissible", default = "true")
    dismissible: Boolean = true,

    @PreviewParam(label = "Severity", options = ["Info", "Warning", "Error"])
    severity: String = "Info",
) {
    AlertBanner(message = message, dismissible = dismissible, severity = severity)
}
```

---

## Defining preview groups

Groups are sealed hierarchies of `PreviewGroup` implementations. The library only ships the `PreviewGroup` interface — you define the tree.

```kotlin
sealed interface AppGroup : PreviewGroup {

    // Leaf group
    data object Buttons : AppGroup {
        override val name = "Buttons"
    }

    // Nested group with children
    sealed interface Forms : AppGroup {
        override val name get() = "Forms"

        data object Inputs : Forms {
            override val name = "Inputs"
        }
        data object Pickers : Forms {
            override val name = "Pickers"
        }
    }

    data object Screens : AppGroup {
        override val name = "Screens"
        override val description = "Full-screen entry points"
    }
}
```

The KSP processor handles arbitrarily deep trees. The browser reconstructs the hierarchy from the `group` references in each `@ComposePreview` annotation.

---

## KSP configuration

Pass KSP arguments in your module's `build.gradle.kts`:

```kotlin
ksp {
    arg("composeum.registryPackage", "com.example.myapp.generated")
    arg("composeum.registryName", "GeneratedPreviewRegistry")   // default
    arg("composeum.includeAndroidPreview", "false")             // default
    arg("composeum.enableKdoc", "false")                        // default
}
```

| Argument                          | Default                    | Description                                                             |
| --------------------------------- | -------------------------- | ----------------------------------------------------------------------- |
| `composeum.registryPackage`       | `<first preview package>.generated` | Package for the generated `GeneratedPreviewRegistry` class    |
| `composeum.registryName`          | `GeneratedPreviewRegistry` | Class name for the generated registry                                   |
| `composeum.includeAndroidPreview` | `false`                    | Also process `@androidx.compose.ui.tooling.preview.Preview` annotations |
| `composeum.enableKdoc`            | `false`                    | Use KDoc for preview description/tag fallbacks and `@param` docs for `@PreviewParam.description` |

### KDoc support

KDoc fallback support is opt-in:

```kotlin
ksp {
    arg("composeum.enableKdoc", "true")
}
```

Precedence rules:

1. Explicit annotation values win.
2. Otherwise KDoc fallback is used.
3. Otherwise the generated value is empty.

Supported KDoc fallbacks:

- function summary paragraph -> preview description
- `@param name ...` -> `@PreviewParam.description`
- `@tag foo` / `@tags foo, bar` -> preview tags

Example:

```kotlin
/**
 * Primary call-to-action button.
 *
 * @param label Text shown on the button.
 * @tags button, cta
 */
@ComposePreview(group = AppGroup.Components::class)
@Composable
fun PrimaryButtonPreview(
    @PreviewParam(label = "Label")
    label: String = "Continue",
) { ... }
```

With `composeum.enableKdoc=true`, the generated preview will use the function-name fallback
for `name`, the KDoc summary for `description`, the KDoc tags for `tags`, and the KDoc
`@param` text for the preview parameter description.

### Generated files

KSP generates two kinds of files in `build/generated/`:

**`GeneratedPreviewRegistry.kt`** — the registry object that holds all entries:

```kotlin
// Do not edit — generated by Composeum KSP processor
object GeneratedPreviewRegistry : PreviewRegistry {
    override val entries: List<PreviewEntry> = listOf(
        PreviewEntry(
            key = "com.example.AlertBannerPreview",
            name = "Alert Banner",
            group = AppGroup.Components,
            composable = { AlertBannerPreview() },
            paramForm = { state, onUpdate -> AlertBannerPreviewParamForm(state, onUpdate) },
            paramDefaults = AlertBannerPreviewDefaults,
            sourceFile = "AlertBannerPreview.kt",
            sourceLine = 42,
        ),
        // ...
    )
}
```

**`{FunctionName}ParamForm.kt`** — a `@Composable` that renders the parameter widgets for one preview.

Never edit these files manually; they are regenerated on every KSP run.

---

## Launching the browser

### Option A — extend `ComposeumBrowserActivity`

The simplest path. Extend and supply a registry and config:

```kotlin
class PreviewActivity : ComposeumBrowserActivity() {
    override val registry = GeneratedPreviewRegistry
    override val config = previewConfig { /* ... */ }
}
```

### Option B — embed `ComposeumBrowser` composable

Use this when you need the browser inside an existing Compose hierarchy (e.g. a debug drawer):

```kotlin
@Composable
fun DebugScreen() {
    ComposeumBrowser(
        registry = GeneratedPreviewRegistry,
        config = previewConfig { /* ... */ },
        modifier = Modifier.fillMaxSize(),
    )
}
```

`ComposeumBrowser` owns its own `NavHost` and handles all internal navigation.

---

## Parameter controls

When a composable has `@PreviewParam` parameters, a **Param Panel** slide-out appears in the browser. The panel renders a widget per parameter based on the parameter type:

| Kotlin type                                  | Widget                 | Notes                                                  |
| -------------------------------------------- | ---------------------- | ------------------------------------------------------ |
| `String`                                     | Text field             | `options` non-empty → dropdown                         |
| `Boolean`                                    | Switch                 |                                                        |
| `Int`                                        | Number field + stepper |                                                        |
| `Float`                                      | Number field + stepper |                                                        |
| `androidx.compose.ui.unit.Dp`                | Dp field               |                                                        |
| `androidx.compose.ui.unit.TextUnit`          | Sp field               |                                                        |
| `androidx.compose.ui.graphics.Color`         | Colour picker          | `default` is a hex string e.g. `"FF5722"`              |
| Any type with a registered `customTypeField` | Custom widget          | See [custom type fields](#advanced-custom-type-fields) |

The `options` array on `@PreviewParam` overrides the widget for any type to a dropdown:

```kotlin
@PreviewParam(label = "Variant", options = ["Primary", "Secondary", "Destructive"])
variant: String = "Primary",
```

---

## Advanced: hand-built registry

You don't need KSP to use the runtime. Build a registry manually using the `buildRegistry` DSL — useful for design-system packages, Kotlin Multiplatform, or testing:

```kotlin
val MyDslRegistry: PreviewRegistry = buildRegistry {

    // Preview with no parameters
    preview(
        name = "DSL Greeting",
        group = AppGroup.Components,
        description = "Hello from the DSL",
        tags = listOf("text"),
    ) {
        Text("Hello, Composeum!")
    }

    // Preview with parameters
    preview(
        name = "DSL Button",
        group = AppGroup.Buttons,
        params = previewParams {
            string("label", default = "Click me")
            boolean("enabled", default = true)
        },
    ) { state ->
        MyButton(
            label = state["label"] ?: "Click me",
            enabled = state["enabled"] ?: true,
        )
    }

    // Preview with a dropdown
    preview(
        name = "DSL Card",
        group = AppGroup.Components,
        params = previewParams {
            dropdown("style", options = listOf("Filled", "Outlined", "Elevated"), default = "Filled")
            color("tint", default = 0xFF6200EE)
        },
    ) { state ->
        val style: String = state["style"] ?: "Filled"
        val tint: Color = state["tint"] ?: Color(0xFF6200EE)
        MyCard(style = style, tint = tint)
    }
}
```

### Combining registries

Merge a generated registry with a hand-built one using `CompositePreviewRegistry`:

```kotlin
val registry = CompositePreviewRegistry(GeneratedPreviewRegistry, MyDslRegistry)
```

Duplicate keys (same fully-qualified function name) are deduplicated — the first registry wins.

---

## Advanced: custom type fields

For non-primitive parameter types, register a custom widget in the config:

```kotlin
// Your custom type
enum class AlertSeverity { Info, Warning, Error }

// Register a widget for it
previewConfig {
    customTypeField(initialValue = AlertSeverity.Info) { current, onUpdate ->
        // Your widget composable
        Row {
            AlertSeverity.entries.forEach { severity ->
                FilterChip(
                    selected = current == severity,
                    onClick = { onUpdate(severity) },
                    label = { Text(severity.name) },
                )
            }
        }
    }
}
```

Then annotate parameters of that type normally:

```kotlin
@ComposePreview(name = "Alert Badge", group = AppGroup.Components::class)
@Composable
fun AlertBadgePreview(
    @PreviewParam(label = "Severity")
    severity: AlertSeverity = AlertSeverity.Info,
) {
    AlertBadge(severity = severity)
}
```

---

## Advanced: config DSL reference

All browser behaviour is controlled by the `previewConfig { }` DSL. The config object is immutable after the browser first composes.

```kotlin
val config = previewConfig {

    // --- Display defaults ---
    thumbnailColumns = 2          // columns in the group thumbnail grid
    showDescriptions = true       // show description text under preview names
    showTags = false              // show tag chips
    showParamPanel = true         // show the param panel by default
    groupExpansionMode = GroupExpansionMode.SUBSCREEN  // or INLINE

    // --- Locale options shown in settings ---
    localeOptions = listOf(
        LocaleOption("en", "English"),
        LocaleOption("de", "Deutsch"),
        LocaleOption("fr", "Français"),
        LocaleOption("ja", "Japanese"),
    )

    // --- Top bar actions ---
    topBarAction(
        contentDescription = "About",
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
    ) {
        // onClick handler
        showAboutDialog()
    }

    // --- Source code links ---
    sourceBaseUrl = "https://github.com/myorg/myapp/blob/main/"
    sourceStripPrefix = "/home/runner/work/myapp/myapp/"

    // --- Custom settings panel items ---
    settingsItems = listOf(
        SettingItem.BuiltIn(BuiltInSettingId.THEME),
        SettingItem.BuiltIn(BuiltInSettingId.FONT_SCALE),
        SettingItem.BuiltIn(BuiltInSettingId.UI_SCALE),
        SettingItem.BuiltIn(BuiltInSettingId.LOCALE),
        SettingItem.Custom("reset") {
            // Any composable, e.g. a reset button
            OutlinedButton(onClick = { /* reset */ }) { Text("Reset all") }
        },
    )

    // --- Custom type widgets ---
    customTypeField(initialValue = AlertSeverity.Info) { current, onUpdate -> /* widget */ }
}
```

### `GroupExpansionMode`

| Value       | Behaviour                                                      |
| ----------- | -------------------------------------------------------------- |
| `SUBSCREEN` | Tapping a group navigates to a new screen listing its previews |
| `INLINE`    | Groups expand/collapse in place on the group list screen       |

---

## Advanced: per-group overrides

Override display settings for individual groups using the `groups { }` block:

```kotlin
previewConfig {
    groups {
        group(AppGroup.Screens) {
            thumbnailColumns = 1
            expansionMode = GroupExpansionMode.SUBSCREEN
            previewWrapper { content ->
                // Wrap every Screens preview with a phone frame
                PhoneFrame { content() }
            }
        }

        group(AppGroup.Forms.Inputs) {
            thumbnailColumns = 2
            groupWrapper { header, content ->
                Column {
                    header()
                    Divider()
                    content()
                }
            }
        }
    }
}
```

Per-group overrides take precedence over the top-level defaults.

---

## Advanced: wrapping the browser

Three wrapper hooks let you inject your own Compose hierarchy at different levels.

### `browserWrapper` — outermost, applied once

Wraps the entire browser. Use this to provide your app's `MaterialTheme` so previews render with the correct tokens:

```kotlin
browserWrapper { content ->
    MyAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
```

The `LocalIsDarkTheme` composition local inside the wrapper reflects the current dark-mode setting and updates live when the user toggles it in the settings panel.

### `groupWrapper` — applied once per group screen

```kotlin
groupWrapper { header, content ->
    Scaffold(topBar = { header() }) { padding ->
        content(Modifier.padding(padding))
    }
}
```

### `previewWrapper` — applied once per preview card

```kotlin
previewWrapper { content ->
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        content()
    }
}
```

All three wrappers can be overridden per-group in `groups { group(...) { } }`.

---

## Advanced: source code links

When configured, a link icon appears in the preview detail screen that opens the source file in your browser.

```kotlin
previewConfig {
    // Base URL of your repository's file viewer
    sourceBaseUrl = "https://github.com/myorg/myapp/blob/main/"

    // Prefix stripped from the absolute paths recorded by KSP
    // (KSP records the absolute path on the build machine)
    sourceStripPrefix = "/home/runner/work/myapp/myapp/"
}
```

The final URL is constructed as `sourceBaseUrl + (absolutePath - sourceStripPrefix) + "#L{line}"`.

---

## Multi-module projects

In a multi-module project, run KSP in each module that contains `@ComposePreview` annotations and merge the registries in your browser app:

**`:feature-auth/build.gradle.kts`**

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("tech.lucam.composeum:preview-annotation:0.1.0")
    ksp("tech.lucam.composeum:preview-ksp:0.1.0")
}

ksp {
    arg("composeum.registryPackage", "com.example.auth.generated")
    arg("composeum.registryName", "AuthPreviewRegistry")
}
```

**`:feature-feed/build.gradle.kts`**

```kotlin
ksp {
    arg("composeum.registryPackage", "com.example.feed.generated")
    arg("composeum.registryName", "FeedPreviewRegistry")
}
```

**`:preview-app/MainActivity.kt`**

```kotlin
class MainActivity : ComposeumBrowserActivity() {
    override val registry = CompositePreviewRegistry(
        AuthPreviewRegistry,
        FeedPreviewRegistry,
        ProfilePreviewRegistry,
    )
    override val config = previewConfig { /* ... */ }
}
```

Each module's KSP output is independent. `CompositePreviewRegistry` merges them at runtime and deduplicates any overlapping keys.

---

## Error isolation

Every composable rendered inside the browser is wrapped in crash-isolation logic. A broken or throwing composable displays an inline error card instead of crashing the browser.

This means you can safely browse previews while some are in a broken state — useful during active development.

---

## Known limitations

- The hosted browser flow is Android-first today. `preview-runtime` also contains shared/runtime pieces for Compose Multiplatform, but there is no native SwiftUI host and no iOS browser activity equivalent.
- `@PreviewParam` supports primitives, enums, `Color`, `Dp`, `TextUnit`, lists of supported scalar values, and shallow data/sealed expansions. Deeply nested object graphs still require `previewConfig { customTypeField(...) }`.
- `@ViewPreview` is intentionally narrow: it supports functions returning `android.view.View`, optionally with a single `Context` parameter, and it does not support `@PreviewParam`.
- Composeum generates registries at compile time. If KSP is disabled or misconfigured for a source set, those previews will not appear in the browser.
- Android Studio `@Preview` import support is opt-in through `composeum.includeAndroidPreview=true`; by default only `@ComposePreview` and `@ViewPreview` are collected.

---

## Module architecture

```
composeum/
├── preview-annotation/         # @ComposePreview, @PreviewParam, PreviewGroup
│   └── src/commonMain/         # KMP: jvm + wasmJs — zero runtime dependencies
│
├── preview-ksp/                # KSP processor
│   └── src/main/               # JVM-only, compile-time only
│
├── preview-runtime/            # Registry model, config DSL, browser UI
│   └── src/
│       ├── commonMain/         # Registry, DSL, all Compose UI
│       ├── androidMain/        # DataStoreSettingsStorage, Activity, Log adapter
│       └── wasmJsMain/         # LocalStorageSettingsStorage, console.log adapter
│
└── sample/                     # Example Android app — reference implementation
```

### Dependency rules

| Module                | May depend on                                                       |
| --------------------- | ------------------------------------------------------------------- |
| `:preview-annotation` | Kotlin stdlib only                                                  |
| `:preview-ksp`        | `:preview-annotation`, KSP API, KotlinPoet — **never runtime**      |
| `:preview-runtime`    | `:preview-annotation`, Compose Multiplatform, DataStore, Navigation |
| `:sample`             | All three + your own app code                                       |

`:preview-ksp` is always a `ksp(...)` dependency, never `implementation(...)`.

### Package layout

```
tech.lucam.composeum.annotation          # :preview-annotation public API
tech.lucam.composeum.ksp                 # :preview-ksp internals
tech.lucam.composeum.runtime             # :preview-runtime public API
tech.lucam.composeum.runtime.ui          # browser composables
tech.lucam.composeum.runtime.ui.widgets  # param form widgets
tech.lucam.composeum.runtime.config      # PreviewConfig, DSL, GroupConfig
tech.lucam.composeum.runtime.store       # DataStore keys, settings model
```

---

## Building locally

**Prerequisites:** JDK 17+, Android SDK with API 35.

```bash
git clone https://github.com/lucam-tech/composeum.git
cd composeum

# Build everything
./gradlew build

# Run the sample app (connects a device or emulator first)
./gradlew :sample:installDebug

# Run unit tests
./gradlew test

# Run KSP processor tests
./gradlew :preview-ksp:test

# Publish to local Maven
./gradlew publishToMavenLocal
```

### Version catalog

All dependency versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml). Never hardcode versions in module `build.gradle.kts` files.

Key versions:

| Dependency            | Version       |
| --------------------- | ------------- |
| Kotlin                | 2.0.21        |
| KSP                   | 2.0.21-1.0.28 |
| Compose BOM           | 2024.12.01    |
| Compose Multiplatform | 1.7.1         |
| Android Gradle Plugin | 8.13.2        |
| DataStore             | 1.1.1         |
| Navigation            | 2.8.5         |
| KotlinPoet            | 2.0.0         |
