package tech.lucam.composeum.ksp.model

/**
 * Internal representation of a single @ComposePreview-annotated function,
 * carrying everything the code generator needs to emit a registry entry.
 */
internal data class PreviewModel(
    /** Fully-qualified function name used as the registry key. */
    val key: String,
    /** Display name from @ComposePreview.name. */
    val name: String,
    /** Kotlin expression for the group object, e.g. "AppGroup.Components.Buttons". */
    val groupExpression: String,
    /** Fully-qualified name of the top-level group class for import statements. */
    val groupImport: String,
    /** Optional Kotlin expression for the variant-group object used to group preview flavors. */
    val variantGroupExpression: String? = null,
    /** Fully-qualified name of the top-level variant-group class for import statements. */
    val variantGroupImport: String = "",
    /** Whether this preview should be treated as the base/default flavor in its variant family. */
    val isDefaultVariant: Boolean = false,
    /** Optional description from @ComposePreview.description. */
    val description: String,
    /** Tag constructor/object expressions for the generated registry. */
    val tags: List<String>,
    /** Simple function name used to emit the composable lambda and form reference. */
    val functionSimpleName: String,
    /** Package of the annotated function, used for import statements in generated code. */
    val functionPackage: String,
    /** @PreviewParam-annotated parameters in declaration order. */
    val params: List<ParamModel>,
    /** Flattened state-key to Kotlin-type hints for shareable route serialization. */
    val paramTypeHints: Map<String, String> = emptyMap(),
    /** True when built from a @ViewPreview function rather than @ComposePreview. */
    val isViewPreview: Boolean = false,
    /**
     * True when the @ViewPreview function accepts a single `android.content.Context` parameter.
     * Drives whether the generated AndroidView factory lambda passes `it` to the function.
     */
    val hasContextParam: Boolean = false,
    /** True when built from a Jetpack Compose @Preview annotation (opt-in via KSP arg). */
    val isAndroidPreview: Boolean = false,
    /**
     * Non-null when the registry generator must emit a synthetic PreviewGroup object for this
     * entry instead of referencing a user-defined one.
     */
    val syntheticGroupDisplayName: String? = null,
    /**
     * Original group string from @Preview.group (e.g. "Buttons"). Used to generate a synthetic
     * PreviewGroup object in the registry file. Empty means the default "Android Previews" group.
     */
    val androidPreviewGroupName: String = "",
    /** Absolute path to the Kotlin source file that declares this function, captured by KSP. */
    val sourceFile: String = "",
    /** Line number of the function declaration within [sourceFile]. */
    val sourceLine: Int = 0,
)
