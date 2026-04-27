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
            logger.error("group must implement PreviewGroup", function)
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

        if (!groupImplementsPreviewGroup(function, resolver, VIEW_PREVIEW_FQN)) {
            logger.error("group must implement PreviewGroup", function)
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

            if (strictTypes) {
                @Suppress("UNCHECKED_CAST")
                val options = previewParamAnn.arguments
                    .firstOrNull { it.name?.asString() == "options" }
                    ?.value as? List<*>
                    ?: emptyList<Any>()

                if (options.isEmpty()) {
                    val paramType = param.type.resolve()
                    val declaration = paramType.declaration
                    val isEnum = (declaration as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS
                    val fqn = declaration.qualifiedName?.asString()

                    if (!isEnum && fqn !in SUPPORTED_TYPE_FQNS) {
                        val isList = fqn == "kotlin.collections.List"
                        val isDataClass = (declaration as? KSClassDeclaration)
                            ?.let { Modifier.DATA in it.modifiers } == true
                        val isSealed = (declaration as? KSClassDeclaration)
                            ?.let { Modifier.SEALED in it.modifiers } == true
                        if (!isList && !isDataClass && !isSealed) {
                            val typeName = fqn ?: paramType.toString()
                            // Custom (non-primitive) types are handled at runtime via
                            // PreviewConfig.customTypeFields. Emit a warning so the developer
                            // knows they must register a CustomParamField for this type.
                            logger.warn(
                                "@PreviewParam: '$typeName' is a custom type — register a " +
                                    "customTypeField<${typeName.substringAfterLast('.')}> in PreviewConfig " +
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
}
