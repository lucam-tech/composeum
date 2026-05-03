# 03 — Technical Specification

---

## 3.1 Repository Layout

```
composeum/
├── build-logic/                         # Convention plugins
│   ├── settings.gradle.kts
│   └── src/main/kotlin/
│       ├── android-library.gradle.kts   # Shared Android lib config
│       └── kotlin-library.gradle.kts   # Pure Kotlin lib config
├── gradle/
│   └── libs.versions.toml              # Single source of truth for versions
├── preview-annotation/                  # :preview-annotation module
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/tech/lucam/composeum/annotation/
│       ├── ComposePreview.kt
│       ├── PreviewParam.kt
│       ├── PreviewGroup.kt
│       └── ViewPreview.kt
├── preview-ksp/                         # :preview-ksp module
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/tech/lucam/composeum/ksp/
│       │   ├── ComposeumProcessor.kt
│       │   ├── ComposeumProcessorProvider.kt
│       │   ├── model/                   # Internal KSP data classes
│       │   ├── codegen/                 # Code generation functions
│       │   │   ├── RegistryGenerator.kt
│       │   │   └── ParamFormGenerator.kt
│       │   └── validation/
│       │       └── Validator.kt
│       └── test/kotlin/tech/lucam/composeum/ksp/
│           └── ComposeumProcessorTest.kt
├── preview-runtime/                     # :preview-runtime module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/tech/lucam/composeum/runtime/
│       │   ├── PreviewEntry.kt
│       │   ├── PreviewRegistry.kt
│       │   ├── PreviewParamState.kt
│       │   ├── config/
│       │   ├── store/
│       │   │   ├── RuntimeSettings.kt
│       │   │   ├── SettingsStorage.kt
│       │   │   └── SettingsViewModel.kt
│       │   └── ui/
│       │       ├── ComposeumBrowser.kt       # Public multiplatform entry point composable
│       │       ├── screen/
│       │       ├── component/
│       │       └── widgets/
│       ├── androidMain/kotlin/tech/lucam/composeum/runtime/
│       │   ├── store/DataStoreSettingsStorage.kt
│       │   ├── ui/ComposeumBrowserActivity.kt
│       │   ├── ui/ComposeumBrowserAndroid.kt
│       │   └── util/Log.kt
│       ├── wasmJsMain/kotlin/tech/lucam/composeum/runtime/
│       │   ├── store/LocalStorageSettingsStorage.kt
│       │   ├── ui/screen/GroupNodeWasmJs.kt
│       │   └── util/Log.kt
│       └── androidUnitTest/kotlin/tech/lucam/composeum/runtime/
├── sample/                              # :sample app
│   ├── build.gradle.kts
│   └── src/main/kotlin/tech/lucam/composeum/sample/
│       ├── MainActivity.kt
│       ├── SampleGroup.kt              # User-defined sealed group hierarchy
│       └── previews/                   # Sample @ComposePreview functions
├── settings.gradle.kts
├── build.gradle.kts                    # Root build file
└── CLAUDE.md
```

---

## 3.2 Gradle Configuration

### `gradle/libs.versions.toml` (key entries)

```toml
[versions]
kotlin               = "2.0.21"
ksp                  = "2.0.21-1.0.28"
compose-bom          = "2024.12.01"
android-gradle       = "8.7.3"
ksp-api              = "2.0.21-1.0.28"
datastore            = "1.1.1"
collections-immutable = "0.3.8"
compile-testing-ksp  = "1.6.0"
junit                = "4.13.2"
robolectric          = "4.13"
navigation-compose   = "2.8.5"
compose-multiplatform = "1.8.2"

[libraries]
compose-bom               = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui                = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling        = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3         = { group = "androidx.compose.material3", name = "material3" }
compose-foundation        = { group = "androidx.compose.foundation", name = "foundation" }
navigation-compose-cmp    = { group = "org.jetbrains.androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
datastore-preferences     = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
collections-immutable     = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "collections-immutable" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.7.3" }
ksp-api                   = { group = "com.google.devtools.ksp", name = "symbol-processing-api", version.ref = "ksp-api" }
compile-testing-ksp       = { group = "com.github.tschuchortdev", name = "kotlin-compile-testing-ksp", version.ref = "compile-testing-ksp" }
junit                     = { group = "junit", name = "junit", version.ref = "junit" }
robolectric               = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
kotlin-poet               = { group = "com.squareup", name = "kotlinpoet", version = "1.18.1" }
kotlin-poet-ksp           = { group = "com.squareup", name = "kotlinpoet-ksp", version = "1.18.1" }
```

### Convention plugins

**`preview-annotation/build.gradle.kts`**:

- `kotlin("multiplatform")`
- targets: `jvm()`, `wasmJs { browser() }`
- no Compose or Android dependencies

**`preview-runtime/build.gradle.kts`**:

- `org.jetbrains.kotlin.multiplatform`
- `com.android.library`
- `org.jetbrains.kotlin.plugin.compose`
- targets: `androidTarget`, `wasmJs { browser() }`
- common UI uses Compose Multiplatform + `org.jetbrains.androidx.navigation:navigation-compose`
- `androidMain` adds DataStore + Activity Compose
- `wasmJsMain` adds `kotlinx-serialization-json`

**`preview-ksp/build.gradle.kts`**:

- JVM-only Kotlin library
- compile-time processor only; no runtime target

---

## 3.3 KSP Processor Design

### Entry point

`ComposeumProcessorProvider` implements `SymbolProcessorProvider` and instantiates
`ComposeumProcessor`. Registered via
`resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.

### Processing flow

```
ComposeumProcessor.process(resolver)
  │
  ├─ 1. resolver.getSymbolsWithAnnotation("...ComposePreview")
  │       → List<KSFunctionDeclaration>
  │       → includes declarations compiled from `commonMain`
  │
  ├─ 2. Validator.validate(function) for each declaration
  │       → emit KSP error and skip on violation (never throw)
  │
  ├─ 3. Build internal PreviewModel per valid function
  │       → resolves group KClass to its object instance name
  │       → collects @PreviewParam parameters with types
  │       → may fall back to KDoc when `composeum.enableKdoc=true`
  │
  ├─ 4. RegistryGenerator.generate(models, env)
  │       → writes GeneratedPreviewRegistry.kt
  │
  └─ 5. ParamFormGenerator.generate(model, env) for each model with params
          → writes {FunctionName}ParamForm.kt
```

### Validation rules (in `Validator.kt`)

| Rule                                                          | KSP error message                                                                                                                                                         |
|---------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Function is not `@Composable`                                 | `"@ComposePreview can only be applied to @Composable functions"`                                                                                                          |
| `group` KClass does not implement `PreviewGroup`              | `"group must implement PreviewGroup"`                                                                                                                                     |
| `@PreviewParam` on unsupported type (with `strictTypes=true`) | `"@PreviewParam: unsupported type {type}. Supported: String, Boolean, Int, Long, Float, Double, Color, Enum. Add options=[] to use a dropdown, or remove @PreviewParam."` |
| `@PreviewParam` parameter has no default value                | `"@PreviewParam parameter '{name}' must have a default value in the function signature"`                                                                                  |

### Multiplatform source-set rule

- `@ComposePreview` is expected in `commonMain` or any platform source set.
- `@ViewPreview` is valid only in Android-capable compilations.
- Generated `AndroidView { ... }` wrappers must only be emitted when the processor is running in an
  Android compilation that can resolve `android.view.View` and
  `androidx.compose.ui.viewinterop.AndroidView`.
- wasm/web-targeted compilations must never contain generated code that references Android framework
  types.

### KSP arguments

| Argument                          | Default                              | Description                                         |
|-----------------------------------|--------------------------------------|-----------------------------------------------------|
| `composeum.registryPackage`       | `{module-package}.preview.generated` | Package for generated registry                      |
| `composeum.registryName`          | `GeneratedPreviewRegistry`           | Class name of registry                              |
| `composeum.strictTypes`           | `"true"`                             | Fail on unsupported `@PreviewParam` types           |
| `composeum.includeAndroidPreview` | `"false"`                            | Also process Jetpack Compose `@Preview` annotations |
| `composeum.enableKdoc`            | `"false"`                            | Use KDoc for description/tag/`@param` fallbacks     |

### Generated code format

**Registry file** (`GeneratedPreviewRegistry.kt`):

```kotlin
// DO NOT EDIT — generated by composeum KSP processor
package com.example.preview.generated

import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewRegistry
// ... other imports

public object GeneratedPreviewRegistry : PreviewRegistry {
    override val entries: List<PreviewEntry> = listOf(
        PreviewEntry(
            key         = "com.example.ui.PrimaryButtonPreview",
            name        = "Primary Button",
            group       = AppGroup.Components.Buttons,
            description = "The main CTA button.",
            tags        = listOf("cta", "interactive"),
            composable  = { PrimaryButtonPreview() },
            paramForm   = { state, onUpdate ->
                              PrimaryButtonPreviewParamForm(state, onUpdate)
                          },
            paramDefaults = PreviewParamDefaults(
                defaults = mapOf(
                    "label"   to "Click me",
                    "enabled" to true,
                    "loading" to false,
                )
            ),
        ),
    )
}
```

**Param form file** (`PrimaryButtonPreviewParamForm.kt`):

```kotlin
// DO NOT EDIT — generated by composeum KSP processor
package com.example.preview.generated

@Composable
internal fun PrimaryButtonPreviewParamForm(
    state: PreviewParamState,
    onUpdate: (PreviewParamState) -> Unit,
) {
    PreviewStringField(
        label   = "Label",
        value   = state["label"] ?: "Click me",
        onValue = { onUpdate(state.put("label", it)) },
    )
    PreviewBooleanField(
        label   = "Enabled",
        value   = state["enabled"] ?: true,
        onValue = { onUpdate(state.put("enabled", it)) },
    )
    PreviewBooleanField(
        label   = "Loading",
        value   = state["loading"] ?: false,
        onValue = { onUpdate(state.put("loading", it)) },
    )
}
```

KotlinPoet is used for all code generation. Never use string templates to emit Kotlin source.

---

## 3.4 Browser Navigation

Uses Compose Multiplatform Navigation via `org.jetbrains.androidx.navigation:navigation-compose`.
The API mirrors the AndroidX navigation-compose surface closely, so the browser code remains
shared in `commonMain`. Routes are defined as a sealed class.

```kotlin
sealed class PreviewRoute(val route: String) {
    data object GroupList : PreviewRoute("group_list")
    data class PreviewList(val groupKey: String) :
        PreviewRoute("preview_list/{groupKey}") {
        companion object { const val ARG = "groupKey" }
    }
    data class PreviewDetail(val entryKey: String) :
        PreviewRoute("preview_detail/{entryKey}") {
        companion object { const val ARG = "entryKey" }
    }
}
```

Deep links: `preview://group/{groupKey}` and `preview://entry/{entryKey}`.

---

## 3.5 Preview Rendering

`PreviewRenderer` wraps the user's composable:

```kotlin
@Composable
fun PreviewRenderer(entry: PreviewEntry, modifier: Modifier = Modifier) {
    var error by remember { mutableStateOf<Throwable?>(null) }

    if (error != null) {
        PreviewErrorCard(throwable = error!!, modifier = modifier)
        return
    }

    // Scale overrides applied here via CompositionLocalProvider
    Box(modifier = modifier) {
        runCatching {
            entry.composable()
        }.onFailure {
            error = it
        }
    }
}
```

Note: full crash isolation is not currently implemented in the shared runtime; the browser relies on
normal Compose composition behavior and targeted defensive handling around generated/stateful paths.

---

## 3.6 Settings Persistence

`SettingsStorage` is the common persistence contract. It exposes:

```kotlin
val settings: Flow<RuntimeSettings>
suspend fun update(block: RuntimeSettings.() -> RuntimeSettings)
suspend fun reset()
```

Platform implementations:

- Android: `DataStoreSettingsStorage`
- wasmJs: `LocalStorageSettingsStorage`

`SettingsViewModel` holds a reference to `SettingsStorage` and the `PreviewConfig`. It exposes
`resolvedSettings: StateFlow<ResolvedSettings>`.

On Android, the convenience `ComposeumBrowser(...)` overload creates a `DataStoreSettingsStorage`.
On wasmJs, callers construct `LocalStorageSettingsStorage()` themselves and pass it to the common
`ComposeumBrowser(registry, config, storage)` entry point.

---

## 3.7 Config DSL

```kotlin
fun previewConfig(block: PreviewConfigBuilder.() -> Unit): PreviewConfig =
    PreviewConfigBuilder().apply(block).build()

class PreviewConfigBuilder {
    var fontScale: Float = 1.0f
    var uiScale: Float = 1.0f
    var isDarkMode: Boolean? = null
    var locale: java.util.Locale? = null
    var showDescription: Boolean = true
    var showTags: Boolean = true
    var showParamPanel: Boolean = true
    var thumbnailColumns: Int = 2

    private var browserWrapper: BrowserWrapper? = null
    private var groupWrapper: GroupWrapper? = null
    private var previewWrapper: PreviewWrapper? = null
    private val groupOverrides = mutableMapOf<KClass<out PreviewGroup>, GroupConfig>()

    fun browserWrapper(block: BrowserWrapper) { browserWrapper = block }
    fun groupWrapper(block: GroupWrapper) { groupWrapper = block }
    fun previewWrapper(block: PreviewWrapper) { previewWrapper = block }

    fun groups(block: GroupOverrideBuilder.() -> Unit) {
        GroupOverrideBuilder(groupOverrides).apply(block)
    }

    fun build(): PreviewConfig = PreviewConfig(
        fontScale       = fontScale,
        uiScale         = uiScale,
        isDarkMode      = isDarkMode,
        locale          = locale,
        showDescription = showDescription,
        showTags        = showTags,
        showParamPanel  = showParamPanel,
        thumbnailColumns = thumbnailColumns,
        browserWrapper  = browserWrapper,
        groupWrapper    = groupWrapper,
        previewWrapper  = previewWrapper,
        groupOverrides  = groupOverrides,
    )
}

class GroupOverrideBuilder(
    private val overrides: MutableMap<KClass<out PreviewGroup>, GroupConfig>
) {
    fun <G : PreviewGroup> group(
        groupClass: KClass<G>,
        block: GroupConfigBuilder.() -> Unit,
    ) {
        overrides[groupClass] = GroupConfigBuilder().apply(block).build()
    }
}
```

---

## 3.8 Embedding vs Standalone

**Standalone** — user creates a `:catalog` module with:

```kotlin
class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ComposeumBrowser(registry = GeneratedPreviewRegistry, config = myConfig) }
    }
}
```

**Web / wasmJs** — user calls the common browser composable from their own wasm entry point:

```kotlin
renderComposable(rootElementId = "root") {
    ComposeumBrowser(
        registry = GeneratedPreviewRegistry,
        config = myConfig,
        storage = LocalStorageSettingsStorage(),
    )
}
```

**Embedded Android** — user adds `ComposeumBrowserActivity` to their debug `AndroidManifest.xml`.
`ComposeumBrowserActivity` is shipped in `:preview-runtime` as an open Android-only wrapper:

```kotlin
open class ComposeumBrowserActivity : ComponentActivity() {
    open val registry: PreviewRegistry get() = error("Override registry")
    open val config: PreviewConfig get() = PreviewConfig()
}
```

Users subclass `ComposeumBrowserActivity` to provide their registry and config.

The `:sample` module demonstrates this Android-only integration pattern; it is not itself a CMP
target.

For the consumer-facing support matrix and target-status guidance, see
`08-target-support.md`.
