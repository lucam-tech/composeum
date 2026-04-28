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
            logger.error(
                composePreviewMessage(
                    function,
                    "must be annotated with @Composable",
                    "Add @Composable to '${functionName(function)}' or remove @ComposePreview.",
                ),
                function,
            )
            valid = false
        }

        if (!validateFunctionShape(function, logger, "@ComposePreview")) valid = false

        if (!groupImplementsPreviewGroup(function, resolver)) {
            logger.error(
                composePreviewMessage(
                    function,
                    "declares group '${groupTypeName(function, COMPOSE_PREVIEW_FQN)}', which must implement PreviewGroup",
                    "Change the group argument to a PreviewGroup object.",
                ),
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
            logger.error(
                androidPreviewMessage(
                    function,
                    "must be annotated with @Composable",
                    "Add @Composable to '${functionName(function)}' or remove @Preview.",
                ),
                function,
            )
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
            logger.error(
                viewPreviewMessage(
                    function,
                    "must not be annotated with @Composable",
                    "Remove @Composable from '${functionName(function)}' or replace @ViewPreview with @ComposePreview.",
                ),
                function,
            )
            valid = false
        }

        if (!validateFunctionShape(function, logger, "@ViewPreview")) valid = false

        if (!returnsView(function, resolver)) {
            logger.error(
                viewPreviewMessage(
                    function,
                    "must return android.view.View or a subtype",
                    "Change the return type to android.view.View or move this preview to @ComposePreview.",
                ),
                function,
            )
            valid = false
        }

        val params = function.parameters.toList()
        if (params.size > 1) {
            logger.error(
                viewPreviewMessage(
                    function,
                    "may declare at most one parameter of type android.content.Context",
                    "Remove extra parameters or keep a single Context parameter.",
                ),
                function,
            )
            valid = false
        } else if (params.size == 1 && !isContextType(params[0].type.resolve())) {
            logger.error(
                viewPreviewParamMessage(
                    function,
                    parameterName(params[0]),
                    "must be of type android.content.Context",
                    "Change parameter '${parameterName(params[0])}' to android.content.Context or remove it.",
                ),
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
                    viewPreviewParamMessage(
                        function,
                        paramName,
                        "must not use @PreviewParam",
                        "Remove @PreviewParam from parameter '$paramName'.",
                    ),
                    param,
                )
                valid = false
            }
        }

        if (!groupImplementsPreviewGroup(function, resolver, VIEW_PREVIEW_FQN)) {
            logger.error(
                viewPreviewMessage(
                    function,
                    "declares group '${groupTypeName(function, VIEW_PREVIEW_FQN)}', which must implement PreviewGroup",
                    "Change the group argument to a PreviewGroup object.",
                ),
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
            logger.error(
                functionMessage(
                    annotationName,
                    function,
                    "must not be an extension function",
                    "Move the receiver into a regular parameter or wrap the call in a non-extension preview function.",
                ),
                function,
            )
            valid = false
        }

        if (function.typeParameters.isNotEmpty()) {
            logger.error(
                functionMessage(
                    annotationName,
                    function,
                    "must not declare type parameters",
                    "Extract a concrete preview function with fixed types.",
                ),
                function,
            )
            valid = false
        }

        if (Modifier.SUSPEND in function.modifiers) {
            logger.error(
                functionMessage(
                    annotationName,
                    function,
                    "must not be suspend",
                    "Remove the suspend modifier and call the code synchronously from the preview.",
                ),
                function,
            )
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
                val name = parameterName(param)
                logger.error(
                    previewParamMessage(
                        function,
                        name,
                        "must declare a default value in the function signature",
                        "Add '= ...' to parameter '$name'.",
                    ),
                    param,
                )
                valid = false
            }

            if (param.isVararg) {
                val name = parameterName(param)
                logger.error(
                    previewParamMessage(
                        function,
                        name,
                        "must not be vararg because Composeum does not support vararg preview parameters",
                        "Replace the vararg parameter with a concrete collection or fixed parameter list.",
                    ),
                    param,
                )
                valid = false
            }

            if (!validatePreviewParamDefault(function, param, logger)) valid = false

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
                    val paramName = parameterName(param)
                    logger.error(
                        previewParamMessage(
                            function,
                            paramName,
                            "uses dropdown options with unsupported type '$typeName'",
                            "Use String or enum for dropdown options, or remove the options argument.",
                        ),
                        param,
                    )
                    valid = false
                }

                if (options.isNotEmpty() && !validatePreviewParamOptions(function, param, options, logger)) {
                    valid = false
                }

                if (fqn == "kotlin.collections.List" && !isSupportedListElementType(paramType)) {
                    val elementType = paramType.arguments.firstOrNull()?.type?.resolve()
                    val elementTypeName = elementType?.declaration?.qualifiedName?.asString()
                        ?: elementType?.toString()
                        ?: "unknown"
                    val paramName = parameterName(param)
                    logger.error(
                        previewParamMessage(
                            function,
                            paramName,
                            "uses unsupported list element type '$elementTypeName'",
                            "Use List<String>, List<Boolean>, List<Int>, List<Long>, List<Float>, List<Double>, or List<Enum>.",
                        ),
                        param,
                    )
                    valid = false
                }

                if (isUnsupportedNestedComplexType(function, declaration as? KSClassDeclaration, logger, param)) {
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
                            val paramName = parameterName(param)
                            // Custom (non-primitive) types are handled at runtime via
                            // PreviewConfig.customTypeFields. Emit a warning so the developer
                            // knows they must register a CustomParamField for this type.
                            logger.warn(
                                previewParamMessage(
                                    function,
                                    paramName,
                                    "uses custom type '$typeName'",
                                    "Register previewConfig { customTypeField<$simpleTypeName>(initialValue = ...) { ... } } to provide an initial value and a custom widget.",
                                ),
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
        function: KSFunctionDeclaration,
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

        if (classDecl != null && Modifier.DATA in classDecl.modifiers) {
            val paramName = parameterName(param)
            logger.error(
                previewParamMessage(
                    function,
                    paramName,
                    "does not support string defaults for data class type '$typeName'",
                    "Remove the annotation default and rely on the function signature default value instead.",
                ),
                param,
            )
            return false
        }

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
            val paramName = parameterName(param)
            logger.error(
                previewParamMessage(
                    function,
                    paramName,
                    "has invalid default '$defaultValue' for type '$typeName'",
                    "Change the default string to a valid $typeName value or remove the annotation default.",
                ),
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

    private fun validatePreviewParamOptions(
        function: KSFunctionDeclaration,
        param: com.google.devtools.ksp.symbol.KSValueParameter,
        options: List<*>,
        logger: KSPLogger,
    ): Boolean {
        val optionValues = options.filterIsInstance<String>()
        val paramName = parameterName(param)
        var valid = true

        if (optionValues.any { it.isBlank() }) {
            logger.error(
                previewParamMessage(
                    function,
                    paramName,
                    "has blank dropdown options",
                    "Replace blank options with non-empty strings.",
                ),
                param,
            )
            valid = false
        }

        if (optionValues.size != optionValues.toSet().size) {
            logger.error(
                previewParamMessage(
                    function,
                    paramName,
                    "has duplicate dropdown options",
                    "Remove duplicate values so each option is unique.",
                ),
                param,
            )
            valid = false
        }

        val previewParamAnn = param.annotations.firstOrNull { ann ->
            ann.annotationType.resolve().declaration.qualifiedName?.asString() == PREVIEW_PARAM_FQN
        } ?: return valid

        val defaultValue = previewParamAnn.arguments
            .firstOrNull { it.name?.asString() == "default" }
            ?.value as? String
            ?: ""
        if (defaultValue.isNotEmpty() && defaultValue !in optionValues) {
            logger.error(
                previewParamMessage(
                    function,
                    paramName,
                    "uses default '$defaultValue' that is not present in options $optionValues",
                    "Add '$defaultValue' to the options list or change the default.",
                ),
                param,
            )
            valid = false
        }

        val resolvedType = param.type.resolve()
        val declaration = resolvedType.declaration as? KSClassDeclaration
        if (declaration?.classKind == ClassKind.ENUM_CLASS) {
            val enumValues = declaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .map { it.simpleName.asString() }
                .toSet()
            val invalidOptions = optionValues.filterNot { it in enumValues }
            if (invalidOptions.isNotEmpty()) {
                logger.error(
                    previewParamMessage(
                        function,
                        paramName,
                        "has enum options $invalidOptions that are not valid constants of '${declaration.simpleName.asString()}'",
                        "Use only declared enum constant names in the options list.",
                    ),
                    param,
                )
                valid = false
            }
        }

        return valid
    }

    private fun isListType(typeName: String): Boolean =
        typeName == "kotlin.collections.List"

    private fun isSealedClass(classDecl: KSClassDeclaration?): Boolean =
        classDecl != null && Modifier.SEALED in classDecl.modifiers

    private fun isSupportedListElementType(listType: KSType): Boolean {
        val elementType = listType.arguments.firstOrNull()?.type?.resolve() ?: return false
        val elementDecl = elementType.declaration as? KSClassDeclaration
        val elementTypeName = elementType.declaration.qualifiedName?.asString() ?: elementType.toString()
        val isEnumElement = elementDecl?.classKind == ClassKind.ENUM_CLASS
        return isEnumElement || elementTypeName in setOf(
            "kotlin.String",
            "kotlin.Boolean",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double",
        )
    }

    private fun isUnsupportedNestedComplexType(
        function: KSFunctionDeclaration,
        classDecl: KSClassDeclaration?,
        logger: KSPLogger,
        param: com.google.devtools.ksp.symbol.KSValueParameter,
    ): Boolean {
        val paramName = parameterName(param)
        if (classDecl != null && Modifier.DATA in classDecl.modifiers) {
            return classDecl.primaryConstructor?.parameters?.any { nestedParam ->
                val nestedType = nestedParam.type.resolve()
                if (!isUnsupportedNestedExpansionType(nestedType)) return@any false
                val nestedName = nestedParam.name?.asString() ?: "unknown"
                val nestedTypeName = nestedType.declaration.qualifiedName?.asString() ?: nestedType.toString()
                logger.error(
                    previewParamMessage(
                        function,
                        paramName,
                        "contains unsupported nested field '$nestedName' of type '$nestedTypeName'",
                        "Flatten '$nestedName' into supported scalar fields or register a customTypeField for '$paramName'.",
                    ),
                    param,
                )
                true
            } == true
        }

        if (classDecl != null && Modifier.SEALED in classDecl.modifiers) {
            return classDecl.getSealedSubclasses().any { subtype ->
                subtype.primaryConstructor?.parameters?.any { nestedParam ->
                    val nestedType = nestedParam.type.resolve()
                    if (!isUnsupportedNestedExpansionType(nestedType)) return@any false
                    val nestedName = nestedParam.name?.asString() ?: "unknown"
                    val nestedTypeName = nestedType.declaration.qualifiedName?.asString() ?: nestedType.toString()
                    logger.error(
                        previewParamMessage(
                            function,
                            paramName,
                            "subtype '${subtype.simpleName.asString()}' contains unsupported nested field '$nestedName' of type '$nestedTypeName'",
                            "Flatten '$nestedName' into supported scalar fields or register a customTypeField for '$paramName'.",
                        ),
                        param,
                    )
                    true
                } == true
            }
        }

        return false
    }

    private fun isUnsupportedNestedExpansionType(type: KSType): Boolean {
        val declaration = type.declaration as? KSClassDeclaration ?: return false
        val typeName = declaration.qualifiedName?.asString() ?: type.toString()
        return typeName == "kotlin.collections.List" ||
            Modifier.DATA in declaration.modifiers ||
            Modifier.SEALED in declaration.modifiers
    }

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

    private fun functionName(function: KSFunctionDeclaration): String =
        function.simpleName.asString()

    private fun parameterName(param: com.google.devtools.ksp.symbol.KSValueParameter): String =
        param.name?.asString() ?: "unknown"

    private fun functionMessage(
        annotationName: String,
        function: KSFunctionDeclaration,
        problem: String,
        fix: String,
    ): String = "$annotationName function '${functionName(function)}' $problem. $fix"

    private fun previewParamMessage(
        function: KSFunctionDeclaration,
        paramName: String,
        problem: String,
        fix: String,
    ): String =
        "@ComposePreview function '${functionName(function)}' parameter '$paramName' annotated with @PreviewParam $problem. $fix"

    private fun viewPreviewParamMessage(
        function: KSFunctionDeclaration,
        paramName: String,
        problem: String,
        fix: String,
    ): String =
        "@ViewPreview function '${functionName(function)}' parameter '$paramName' $problem. $fix"

    private fun composePreviewMessage(
        function: KSFunctionDeclaration,
        problem: String,
        fix: String,
    ): String = functionMessage("@ComposePreview", function, problem, fix)

    private fun androidPreviewMessage(
        function: KSFunctionDeclaration,
        problem: String,
        fix: String,
    ): String = functionMessage("@Preview", function, problem, fix)

    private fun viewPreviewMessage(
        function: KSFunctionDeclaration,
        problem: String,
        fix: String,
    ): String = functionMessage("@ViewPreview", function, problem, fix)
}
