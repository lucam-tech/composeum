package tech.lucam.composeum.ksp.validation

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

private const val COMPOSABLE_FQN = "androidx.compose.runtime.Composable"
private const val COMPOSE_PREVIEW_FQN = "tech.lucam.composeum.annotation.ComposePreview"
private const val VIEW_PREVIEW_FQN = "tech.lucam.composeum.annotation.ViewPreview"
private const val ANDROID_PREVIEW_FQN = "androidx.compose.ui.tooling.preview.Preview"
private const val PREVIEW_PARAM_FQN = "tech.lucam.composeum.annotation.PreviewParam"
private const val PREVIEW_GROUP_FQN = "tech.lucam.composeum.annotation.PreviewGroup"
private const val VIEW_FQN = "android.view.View"
private const val CONTEXT_FQN = "android.content.Context"

private val SUPPORTED_TYPE_FQNS = setOf(
    "kotlin.String",
    "kotlin.Boolean",
    "kotlin.Int",
    "kotlin.Long",
    "kotlin.Float",
    "kotlin.Double",
    "androidx.compose.ui.graphics.Color",
    "androidx.compose.ui.unit.Dp",
    "androidx.compose.ui.unit.TextUnit",
)

/** Validates @ComposePreview and @ViewPreview annotated functions against all processor rules. */
internal object Validator {

    /**
     * Returns true if the @ComposePreview function passes all validation rules.
     * Emits KSP errors for each violation found.
     */
    fun validate(
        function: KSFunctionDeclaration,
        logger: KSPLogger,
        resolver: Resolver,
        strictTypes: Boolean,
    ): Boolean {
        var valid = true

        if (!isComposable(function)) {
            logger.error("@ComposePreview can only be applied to @Composable functions", function)
            valid = false
        }

        if (!validateFunctionShape(function, logger, "@ComposePreview")) valid = false

        if (!groupImplementsPreviewGroup(function, resolver)) {
            logger.error(
                "@ComposePreview group '${groupTypeName(function, COMPOSE_PREVIEW_FQN)}' must implement PreviewGroup",
                function,
            )
            valid = false
        }

        if (!validatePreviewParams(function, logger, strictTypes)) valid = false

        return valid
    }

    /**
     * Returns true if the Jetpack Compose @Preview function passes all validation rules.
     * Only checks @Composable presence and @PreviewParam constraints — the group field is a
     * plain String on @Preview, so no PreviewGroup assignability check is performed.
     */
    fun validateAndroidPreview(
        function: KSFunctionDeclaration,
        logger: KSPLogger,
        @Suppress("UNUSED_PARAMETER") resolver: Resolver,
        strictTypes: Boolean,
    ): Boolean {
        var valid = true

        if (!isComposable(function)) {
            logger.error("@Preview can only be applied to @Composable functions", function)
            valid = false
        }

        if (!validateFunctionShape(function, logger, "@Preview")) valid = false

        if (!validatePreviewParams(function, logger, strictTypes)) valid = false

        return valid
    }

    /**
     * Returns true if the @ViewPreview function passes all validation rules.
     * Emits KSP errors for each violation found.
     */
    fun validateViewPreview(
        function: KSFunctionDeclaration,
        logger: KSPLogger,
        resolver: Resolver,
    ): Boolean {
        var valid = true

        if (isComposable(function)) {
            logger.error("@ViewPreview functions must not be @Composable", function)
            valid = false
        }

        if (!validateFunctionShape(function, logger, "@ViewPreview")) valid = false

        if (!returnsView(function, resolver)) {
            logger.error(
                "@ViewPreview function must return android.view.View or a subtype",
                function,
            )
            valid = false
        }

        val params = function.parameters.toList()
        if (params.size > 1) {
            logger.error(
                "@ViewPreview function may have at most one parameter (android.content.Context)",
                function,
            )
            valid = false
        } else if (params.size == 1 && !isContextType(params[0].type.resolve())) {
            logger.error(
                "@ViewPreview single parameter must be of type android.content.Context",
                params[0],
            )
            valid = false
        }

        params.forEach { param ->
            val hasPreviewParam = param.annotations.any { ann ->
                ann.annotationType.resolve().declaration.qualifiedName?.asString() == PREVIEW_PARAM_FQN
            }
            if (hasPreviewParam) {
                val paramName = param.name?.asString() ?: "unknown"
                logger.error(
                    "@ViewPreview does not support @PreviewParam. Remove @PreviewParam from parameter '$paramName'.",
                    param,
                )
                valid = false
            }
        }

        if (!groupImplementsPreviewGroup(function, resolver, VIEW_PREVIEW_FQN)) {
            logger.error(
                "@ViewPreview group '${groupTypeName(function, VIEW_PREVIEW_FQN)}' must implement PreviewGroup",
                function,
            )
            valid = false
        }

        return valid
    }

    private fun returnsView(function: KSFunctionDeclaration, resolver: Resolver): Boolean {
        val returnType = function.returnType?.resolve() ?: return false
        val viewDecl = resolver.getClassDeclarationByName(resolver.getKSNameFromString(VIEW_FQN))
            ?: return true // can't resolve View in this environment — skip check
        return viewDecl.asStarProjectedType().isAssignableFrom(returnType.makeNotNullable())
    }

    private fun isContextType(type: KSType): Boolean =
        type.declaration.qualifiedName?.asString() == CONTEXT_FQN

    private fun isComposable(function: KSFunctionDeclaration): Boolean =
        function.annotations.any { ann ->
            ann.annotationType.resolve().declaration.qualifiedName?.asString() == COMPOSABLE_FQN
        }

    private fun validateFunctionShape(
        function: KSFunctionDeclaration,
        logger: KSPLogger,
        annotationName: String,
    ): Boolean {
        var valid = true

        if (function.extensionReceiver != null) {
            logger.error("$annotationName functions must not be extension functions", function)
            valid = false
        }

        if (function.typeParameters.isNotEmpty()) {
            logger.error("$annotationName functions must not declare type parameters", function)
            valid = false
        }

        if (Modifier.SUSPEND in function.modifiers) {
            logger.error("$annotationName functions must not be suspend", function)
            valid = false
        }

        return valid
    }

    private fun validatePreviewParams(
        function: KSFunctionDeclaration,
        logger: KSPLogger,
        strictTypes: Boolean,
    ): Boolean {
        var valid = true
        for (param in function.parameters) {
            val previewParamAnn = param.annotations.firstOrNull { ann ->
                ann.annotationType.resolve().declaration.qualifiedName?.asString() == PREVIEW_PARAM_FQN
            } ?: continue

            if (!param.hasDefault) {
                val name = param.name?.asString() ?: "unknown"
                logger.error(
                    "@PreviewParam parameter '$name' must have a default value in the function signature",
                    param,
                )
                valid = false
            }

            if (param.isVararg) {
                val name = param.name?.asString() ?: "unknown"
                logger.error(
                    "@PreviewParam parameter '$name' must not be vararg. Composeum does not support vararg preview parameters.",
                    param,
                )
                valid = false
            }

            if (!validatePreviewParamDefault(param, logger)) valid = false

            if (strictTypes) {
                @Suppress("UNCHECKED_CAST")
                val options = previewParamAnn.arguments
                    .firstOrNull { it.name?.asString() == "options" }
                    ?.value as? List<*>
                    ?: emptyList<Any>()

                val paramType = param.type.resolve()
                val declaration = paramType.declaration
                val isEnum = (declaration as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS
                val fqn = declaration.qualifiedName?.asString()

                if (options.isNotEmpty() && !isEnum && fqn != "kotlin.String") {
                    val typeName = fqn ?: paramType.toString()
                    val paramName = param.name?.asString() ?: "unknown"
                    logger.error(
                        "@PreviewParam parameter '$paramName' uses options but has unsupported type '$typeName'. " +
                            "Dropdown options are only supported for String and enum parameters.",
                        param,
                    )
                    valid = false
                }

                if (options.isEmpty()) {
                    if (!isEnum && fqn !in SUPPORTED_TYPE_FQNS) {
                        val isList = fqn == "kotlin.collections.List"
                        val isDataClass = (declaration as? KSClassDeclaration)
                            ?.let { Modifier.DATA in it.modifiers } == true
                        val isSealed = (declaration as? KSClassDeclaration)
                            ?.let { Modifier.SEALED in it.modifiers } == true
                        if (!isList && !isDataClass && !isSealed) {
                            val typeName = fqn ?: paramType.toString()
                            val simpleTypeName = typeName.substringAfterLast('.')
                            val paramName = param.name?.asString() ?: "unknown"
                            // Custom (non-primitive) types are handled at runtime via
                            // PreviewConfig.customTypeFields. Emit a warning so the developer
                            // knows they must register a CustomParamField for this type.
                            logger.warn(
                                "@PreviewParam parameter '$paramName' uses custom type '$typeName'. " +
                                    "Register previewConfig { customTypeField<$simpleTypeName>(initialValue = ...) { ... } } " +
                                    "to provide an initial value and a custom widget.",
                                param,
                            )
                        }
                    }
                }
            }
        }
        return valid
    }

    private fun validatePreviewParamDefault(
        param: com.google.devtools.ksp.symbol.KSValueParameter,
        logger: KSPLogger,
    ): Boolean {
        val previewParamAnn = param.annotations.firstOrNull { ann ->
            ann.annotationType.resolve().declaration.qualifiedName?.asString() == PREVIEW_PARAM_FQN
        } ?: return true

        val defaultValue = previewParamAnn.arguments
            .firstOrNull { it.name?.asString() == "default" }
            ?.value as? String
            ?: ""

        if (defaultValue.isEmpty()) return true

        val resolvedType = param.type.resolve()
        val declaration = resolvedType.declaration
        val typeName = declaration.qualifiedName?.asString() ?: resolvedType.toString()
        val isNullable = resolvedType.isMarkedNullable
        val classDecl = declaration as? KSClassDeclaration
        val isEnum = classDecl?.classKind == ClassKind.ENUM_CLASS

        if (isNullable && defaultValue == "null") return true

        val isValid = when {
            isEnum -> classDecl!!.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .any { it.simpleName.asString() == defaultValue }
            typeName == "kotlin.Boolean" -> defaultValue == "true" || defaultValue == "false"
            typeName == "kotlin.Int" -> defaultValue.toIntOrNull() != null
            typeName == "kotlin.Long" -> defaultValue.toLongOrNull() != null
            typeName == "kotlin.Float" -> defaultValue.toFloatOrNull() != null
            typeName == "kotlin.Double" -> defaultValue.toDoubleOrNull() != null
            typeName == "androidx.compose.ui.graphics.Color" -> defaultValue.toLongOrNull() != null
            typeName == "androidx.compose.ui.unit.Dp" -> defaultValue.toFloatOrNull() != null
            typeName == "androidx.compose.ui.unit.TextUnit" -> defaultValue.toFloatOrNull() != null
            isSealedClass(classDecl) -> classDecl!!.getSealedSubclasses()
                .any { it.simpleName.asString() == defaultValue }
            isListType(typeName) -> validateListDefault(resolvedType, defaultValue)
            else -> true
        }

        if (!isValid) {
            val paramName = param.name?.asString() ?: "unknown"
            logger.error(
                "@PreviewParam parameter '$paramName' has invalid default '$defaultValue' for type '$typeName'.",
                param,
            )
        }

        return isValid
    }

    private fun validateListDefault(type: KSType, rawDefault: String): Boolean {
        val elementType = type.arguments.firstOrNull()?.type?.resolve() ?: return false
        val elementDecl = elementType.declaration as? KSClassDeclaration
        val elementTypeName = elementType.declaration.qualifiedName?.asString() ?: elementType.toString()
        val isEnumElement = elementDecl?.classKind == ClassKind.ENUM_CLASS
        val enumValues = if (isEnumElement) {
            elementDecl!!.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .map { it.simpleName.asString() }
                .toSet()
        } else {
            emptySet()
        }

        return rawDefault
            .split("|")
            .filter { it.isNotEmpty() }
            .all { item ->
                when {
                    isEnumElement -> item in enumValues
                    elementTypeName == "kotlin.String" -> true
                    elementTypeName == "kotlin.Boolean" -> item == "true" || item == "false"
                    elementTypeName == "kotlin.Int" -> item.toIntOrNull() != null
                    elementTypeName == "kotlin.Long" -> item.toLongOrNull() != null
                    elementTypeName == "kotlin.Float" -> item.toFloatOrNull() != null
                    elementTypeName == "kotlin.Double" -> item.toDoubleOrNull() != null
                    else -> false
                }
            }
    }

    private fun isListType(typeName: String): Boolean =
        typeName == "kotlin.collections.List"

    private fun isSealedClass(classDecl: KSClassDeclaration?): Boolean =
        classDecl != null && Modifier.SEALED in classDecl.modifiers

    private fun groupImplementsPreviewGroup(
        function: KSFunctionDeclaration,
        resolver: Resolver,
        annotationFqn: String = COMPOSE_PREVIEW_FQN,
    ): Boolean {
        val ann = function.annotations.firstOrNull { a ->
            a.annotationType.resolve().declaration.qualifiedName?.asString() == annotationFqn
        } ?: return true

        val groupType = ann.arguments
            .firstOrNull { it.name?.asString() == "group" }
            ?.value as? KSType ?: return false

        val previewGroupType = resolver
            .getClassDeclarationByName(resolver.getKSNameFromString(PREVIEW_GROUP_FQN))
            ?.asStarProjectedType() ?: return true

        return previewGroupType.isAssignableFrom(groupType)
    }

    private fun groupTypeName(
        function: KSFunctionDeclaration,
        annotationFqn: String,
    ): String {
        val ann = function.annotations.firstOrNull { a ->
            a.annotationType.resolve().declaration.qualifiedName?.asString() == annotationFqn
        } ?: return "unknown"

        val groupType = ann.arguments
            .firstOrNull { it.name?.asString() == "group" }
            ?.value as? KSType
            ?: return "unknown"

        return groupType.declaration.qualifiedName?.asString()
            ?: groupType.toString()
    }
}
