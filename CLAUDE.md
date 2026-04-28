# CLAUDE.md — Composeum

Read this file in full at the start of every session before doing any work.

---

## Project Overview

**composeum** is a Kotlin Multiplatform Compose preview library. It lets developers annotate composables with `@ComposePreview`, collect them at compile time via KSP, and browse them in a live UI browser with interactive parameter controls on Android and Web (Kotlin/WASM).

The library is split into four Gradle modules. There is no backend, no network layer, and no remote state. Everything is local and compile-time-driven.

---

## Module Map

| Module | Path | Role |
|---|---|---|
| `:preview-annotation` | `preview-annotation/` | `@ComposePreview`, `@PreviewParam`, `PreviewGroup` interface — zero runtime deps; KMP (jvm + wasmJs) |
| `:preview-ksp` | `preview-ksp/` | KSP processor — generates registry and param form files; JVM-only at compile time |
| `:preview-runtime` | `preview-runtime/` | Registry model, config DSL, browser composables, param widgets; KMP (androidTarget + wasmJs) |
| `:sample` | `sample/` | Sample Android app that wires everything together for manual testing; Android-only, not a KMP target |

The `:sample` module plays the role of `:preview-browser-app` described in the spec — users copy this pattern into their own project.

### :preview-runtime source set layout

| Source set | Contents |
|---|---|
| `commonMain` | Registry model, `PreviewConfig` DSL, all UI composables, screens, widgets, `SettingsStorage` interface, `SettingsViewModel`, `ComposeumBrowser` |
| `androidMain` | `DataStoreSettingsStorage`, `ComposeumBrowserActivity`, Android convenience `ComposeumBrowser` overload, `logDebug → Log.d` |
| `wasmJsMain` | `LocalStorageSettingsStorage`, `buildGroupTree` flat-list actual, `logDebug → console.log` |
| `androidUnitTest` | All JUnit4/Robolectric tests |

---

## Key Architectural Rules

These rules are non-negotiable. Every code review checks them.

1. **`:preview-annotation` has zero runtime dependencies.** It must never depend on Compose, KSP API, or any other library. Only the Kotlin stdlib is allowed.

2. **`:preview-ksp` is never on the runtime classpath.** It is always a `ksp(...)` dependency, never `implementation(...)`. It must not import any Compose APIs.

3. **KSP generates; humans don't edit generated files.** All files in `build/generated/` are owned by the processor. Never instruct the user to edit them.

4. **No reflection at runtime.** All composable discovery happens at compile time via KSP. `Class.forName`, `kotlin-reflect`, and similar APIs are banned in `:preview-runtime`.

5. **`@ViewPreview` codegen is Android-only.** The KSP processor may emit `AndroidView { ... }` wrappers only when running against an Android-capable source set. Web-targeted compilations must never receive generated code that references `android.view.View`, `android.content.Context`, or `AndroidView`.

6. **Config is immutable after the browser starts.** `PreviewConfig` is a data class. Mutating it after `ComposeumBrowser` first composes is not supported. Runtime user settings (dark mode, font scale, etc.) are stored separately in `DataStore` as an overlay.

7. **No `@Composable` functions in `:preview-annotation` or `:preview-ksp`.** These modules cannot depend on the Compose runtime.

8. **All `@PreviewParam`-annotated parameters must have a default value in the function signature.** The KSP processor must emit an error if a `@PreviewParam` parameter has no default. This ensures the composable is always callable with no arguments.

9. **Sealed group hierarchy is user-owned.** The library ships only the `PreviewGroup` marker interface. The user defines their own sealed tree. The processor must handle arbitrarily deep trees.

10. **Settings persistence goes through `SettingsStorage`.** `commonMain` depends only on the `SettingsStorage` contract; Android uses `DataStoreSettingsStorage`, while wasmJs uses `LocalStorageSettingsStorage`.

---

## Conventions

### Naming

- Generated registry class: `GeneratedPreviewRegistry` (configurable via KSP arg).
- Generated param form function: `{FunctionName}ParamForm` — e.g. `PrimaryButtonPreviewParamForm`.
- Generated defaults object: `{FunctionName}Defaults` — e.g. `PrimaryButtonPreviewDefaults`.
- KSP argument keys: always prefixed `composeum.` — e.g. `composeum.registryPackage`.

### Package structure

```
tech.lucam.composeum.annotation   # :preview-annotation
tech.lucam.composeum.ksp          # :preview-ksp
tech.lucam.composeum.runtime      # :preview-runtime public API
tech.lucam.composeum.runtime.ui   # browser composables
tech.lucam.composeum.runtime.ui.widgets  # param form widgets
tech.lucam.composeum.runtime.config     # DSL and PreviewConfig
tech.lucam.composeum.runtime.store      # DataStore, settings model
```

### Kotlin style

- Prefer `data class` over hand-written equals/hashCode.
- Use `@JvmOverloads` on public API functions that have default parameters for Java interop.
- All public API symbols must have a KDoc comment — minimum one line.
- No `TODO` or `FIXME` in committed code unless it references a specific `TASK-XXX`.
- No `println` or `System.out` — use the KSP `logger` in the processor and `android.util.Log` (behind a debug flag) in runtime.

### Testing

- Test framework: JUnit 4 + Robolectric.
- KSP processor tests use `com.github.tschuchortdev:kotlin-compile-testing-ksp`.
- Test class naming: `{SubjectClass}Test` — e.g. `ComposeumProcessorTest`.
- Every public DSL builder function must have at least one test.
- Every generated code path in the KSP processor must have at least one compile-testing test.

### Gradle

- All versions live in `gradle/libs.versions.toml`.
- Convention plugins live in `build-logic/` as `*.gradle.kts` precompiled script plugins.
- Never hardcode a version string in a `build.gradle.kts` file — always reference the catalog.

---

## Spec Document Index

| File | Contents |
|---|---|
| `docs/spec/01-user-stories.md` | User stories grouped by persona |
| `docs/spec/02-data-model.md` | Registry model, config model, settings model |
| `docs/spec/03-tech-spec.md` | Module structure, KSP processor design, codegen format |
| `docs/spec/04-api-spec.md` | Public Kotlin API surface (annotations, DSL, composables) |
| `docs/spec/05-tasks.md` | Phased implementation tasks |

---

## Claude Code Commands

Custom slash commands live in `.claude/commands/`. Run them with `/project:<name>`.

| Command | When to use |
|---|---|
| `/project:start` | Beginning of every session — orient yourself |
| `/project:task TASK-XXX` | Implement a specific task |
| `/project:review` | Review all changes since last commit |
| `/project:check` | Run all verification checks |
| `/project:unstuck <error>` | Debug a specific error |
