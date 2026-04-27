package tech.lucam.composeum.ksp.model

/**
 * Internal representation of a single @PreviewParam-annotated parameter,
 * carrying everything the code generator needs to emit a widget call.
 */
internal data class ParamModel(
    /** Parameter name as declared in the function signature. */
    val name: String,
    /** Human-readable label from @PreviewParam.label. */
    val label: String,
    /** Fully-qualified type name, e.g. "kotlin.String". */
    val kotlinType: String,
    /** String-encoded default from @PreviewParam.default (may be empty). */
    val defaultValue: String,
    /** Fixed option list from @PreviewParam.options; non-empty forces a dropdown widget. */
    val options: List<String>,
    /** Optional description from @PreviewParam.description. */
    val description: String,
    /** True when the parameter type is an enum class. */
    val isEnum: Boolean = false,
    /** Enum constant names in declaration order; populated only when [isEnum] is true. */
    val enumValues: List<String> = emptyList(),
    /** True when the parameter is declared as nullable (e.g. String?). */
    val isNullable: Boolean = false,
    /** True when the parameter type is a data class. */
    val isDataClass: Boolean = false,
    /** Constructor params of the data class; populated only when [isDataClass] is true. */
    val dataClassParams: List<ParamModel> = emptyList(),
    /** True when the parameter type is a sealed class or sealed interface. */
    val isSealedClass: Boolean = false,
    /** Direct sealed subtypes; populated only when [isSealedClass] is true. */
    val sealedSubtypes: List<SealedSubtype> = emptyList(),
    /** True when the parameter type is List<T>. */
    val isList: Boolean = false,
    /** Fully-qualified element type; populated only when [isList] is true. */
    val listElementKotlinType: String = "",
    /** True when the list element type is an enum. */
    val listElementIsEnum: Boolean = false,
    /** Enum values of the list element; populated only when [listElementIsEnum] is true. */
    val listElementEnumValues: List<String> = emptyList(),
)
