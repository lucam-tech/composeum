package tech.lucam.composeum.ksp.model

/** Represents one direct subtype of a sealed class or sealed interface. */
internal data class SealedSubtype(
    /** Simple name, e.g. "Loading". */
    val name: String,
    /** Fully-qualified name, e.g. "com.example.UiState.Loading". */
    val qualifiedName: String,
    /** True when the subtype is a Kotlin object (no constructor params). */
    val isObject: Boolean,
    /** Constructor params when the subtype is a class; empty for objects. */
    val params: List<ParamModel>,
)
