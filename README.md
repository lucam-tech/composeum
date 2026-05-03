# Composeum

Composeum is a compile-time Jetpack Compose preview browser. Annotate composables with `@ComposePreview`, run KSP, and browse interactive previews without reflection or manual registration.

Artifacts:

- `tech.lucam.composeum:preview-annotation:0.2.0`
- `tech.lucam.composeum:preview-runtime:0.2.0`
- `tech.lucam.composeum:preview-ksp:0.2.0`

Minimal setup:

```kotlin
dependencies {
    implementation("tech.lucam.composeum:preview-annotation:0.2.0")
    ksp("tech.lucam.composeum:preview-ksp:0.2.0")
    implementation("tech.lucam.composeum:preview-runtime:0.2.0")
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

Full documentation now lives in the Starlight site source under [docs](docs/). Deploy that directory to Vercel and place the public docs URL here.

License: [MIT](LICENSE)
