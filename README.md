# Composeum

Composeum is a compile-time Jetpack Compose preview browser. Annotate composables with `@ComposePreview`, run KSP, and browse interactive previews without reflection or manual registration.

Artifacts:

- `tech.lucam.composeum:preview-annotation:0.3.0`
- `tech.lucam.composeum:preview-runtime:0.3.0`
- `tech.lucam.composeum:preview-ksp:0.3.0`

Minimal setup:

```kotlin
dependencies {
    implementation("tech.lucam.composeum:preview-annotation:0.3.0")
    ksp("tech.lucam.composeum:preview-ksp:0.3.0")
    implementation("tech.lucam.composeum:preview-runtime:0.3.0")
}
```

```kotlin
@ComposePreview(name = "Primary Button", group = AppPreviews.Components::class)
@Composable
fun PrimaryButtonPreview() {
    PrimaryButton(label = "Click me")
}
```

```kotlin
class PreviewCatalogActivity : ComposeumBrowserActivity() {
    override val registry = GeneratedPreviewRegistry
}
```

Get started and check out the full documentation under [docs](docs/)

License: [Apache 2.0](LICENSE)
