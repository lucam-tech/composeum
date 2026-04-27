package tech.lucam.composeum.ksp.model

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier

private const val COMPOSE_PREVIEW_FQN = "tech.lucam.composeum.annotation.ComposePreview"
private const val VIEW_PREVIEW_FQN = "tech.lucam.composeum.annotation.ViewPreview"
private const val ANDROID_PREVIEW_FQN = "androidx.compose.ui.tooling.preview.Preview"
private const val PREVIEW_PARAM_FQN = "tech.lucam.composeum.annotation.PreviewParam"
private const val CONTEXT_FQN = "android.content.Context"

/** Converts a validated @ComposePreview function declaration into a [PreviewModel]. */
internal object ModelBuilder {

    fun build(function: KSFunctionDeclaration): PreviewModel {
        val previewAnn = function.annotations.first { ann ->
            ann.annotationType.resolve().declaration.qualifiedName?.asString() == COMPOSE_PREVIEW_FQN
        }

        val args = previewAnn.arguments.associate { it.name?.asString() to it.value }

        val name = args["name"] as? String ?: ""
        val description = args["description"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val tags = (args["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val groupType = args["group"] as? KSType

        val (groupExpression, groupImport) = resolveGroupExpression(groupType)

        val params = collectPreviewParams(function)

        return PreviewModel(
            key = function.qualifiedName?.asString() ?: "",
            name = name,
            groupExpression = groupExpression,
            groupImport = groupImport,
            description = description,
            tags = tags,
            functionSimpleName = function.simpleName.asString(),
            functionPackage = function.packageName.asString(),
            params = params,
            sourceFile = function.containingFile?.filePath ?: "",
            sourceLine = (function.location as? FileLocation)?.lineNumber ?: 0,
        )
    }

    /**
     * Converts a Jetpack Compose @Preview-annotated function into a [PreviewModel].
     * The group string from @Preview is mapped to a synthetic [PreviewGroup] object that will be
     * emitted alongside the registry. @PreviewParam parameters are supported just like @ComposePreview.
     */
    fun buildAndroidPreview(function: KSFunctionDeclaration): PreviewModel {
        val ann = function.annotations.first { a ->
            a.annotationType.resolve().declaration.qualifiedName?.asString() == ANDROID_PREVIEW_FQN
        }
        val args = ann.arguments.associate { it.name?.asString() to it.value }
        val name = (args["name"] as? String ?: "").ifEmpty { function.simpleName.asString() }
        val groupStr = args["group"] as? String ?: ""
        val groupDisplayName = groupStr.ifEmpty { "Android Previews" }
        val syntheticObjName = syntheticGroupObjectName(groupStr)

        val params = collectPreviewParams(function)

        return PreviewModel(
            key = function.qualifiedName?.asString() ?: "",
            name = name,
            groupExpression = syntheticObjName,
            groupImport = "",
            description = "",
            tags = emptyList(),
            functionSimpleName = function.simpleName.asString(),
            functionPackage = function.packageName.asString(),
            params = params,
            isAndroidPreview = true,
            androidPreviewGroupName = groupDisplayName,
            sourceFile = function.containingFile?.filePath ?: "",
            sourceLine = (function.location as? FileLocation)?.lineNumber ?: 0,
        )
    }

    /** Converts a validated @ViewPreview function declaration into a [PreviewModel]. */
    fun buildViewPreview(function: KSFunctionDeclaration): PreviewModel {
        val ann = function.annotations.first { a ->
            a.annotationType.resolve().declaration.qualifiedName?.asString() == VIEW_PREVIEW_FQN
        }

        val args = ann.arguments.associate { it.name?.asString() to it.value }

        val name = args["name"] as? String ?: ""
        val description = args["description"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val tags = (args["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val groupType = args["group"] as? KSType

        val (groupExpression, groupImport) = resolveGroupExpression(groupType)

        val hasContextParam = function.parameters.firstOrNull()
            ?.type?.resolve()?.declaration?.qualifiedName?.asString() == CONTEXT_FQN

        return PreviewModel(
            key = function.qualifiedName?.asString() ?: "",
            name = name,
            groupExpression = groupExpression,
            groupImport = groupImport,
            description = description,
            tags = tags,
            functionSimpleName = function.simpleName.asString(),
            functionPackage = function.packageName.asString(),
            params = emptyList(),
            isViewPreview = true,
            hasContextParam = hasContextParam,
            sourceFile = function.containingFile?.filePath ?: "",
            sourceLine = (function.location as? FileLocation)?.lineNumber ?: 0,
        )
    }

    private fun collectPreviewParams(function: KSFunctionDeclaration): List<ParamModel> =
        function.parameters.mapNotNull { param ->
            val paramAnn = param.annotations.firstOrNull { ann ->
                ann.annotationType.resolve().declaration.qualifiedName?.asString() == PREVIEW_PARAM_FQN
            } ?: return@mapNotNull null

            val pArgs = paramAnn.arguments.associate { it.name?.asString() to it.value }
            val label = pArgs["label"] as? String ?: ""
            val defaultValue = pArgs["default"] as? String ?: ""
            val paramDescription = pArgs["description"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val options = (pArgs["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            buildParamModel(
                ksParam = param,
                name = param.name?.asString() ?: "",
                label = label,
                defaultValue = defaultValue,
                description = paramDescription,
                options = options,
            )
        }

    /**
     * Builds a [ParamModel] from a KSP parameter, detecting enums, nullability,
     * data classes, sealed classes, and List<T>.
     */
    private fun buildParamModel(
        ksParam: KSValueParameter,
        name: String,
        label: String,
        defaultValue: String,
        description: String,
        options: List<String>,
    ): ParamModel {
        val resolvedType = ksParam.type.resolve()
        val isNullable = resolvedType.isMarkedNullable
        val declaration = resolvedType.declaration
        val kotlinType = declaration.qualifiedName?.asString() ?: ""
        val classDecl = declaration as? KSClassDeclaration

        // Enum
        val isEnum = classDecl?.classKind == ClassKind.ENUM_CLASS
        val enumValues = if (isEnum) {
            classDecl!!.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .map { it.simpleName.asString() }
                .toList()
        } else emptyList()

        // List<T>
        val isList = kotlinType == "kotlin.collections.List"
        val listElementKotlinType = if (isList) {
            resolvedType.arguments.firstOrNull()?.type?.resolve()
                ?.declaration?.qualifiedName?.asString() ?: ""
        } else ""
        val listElementClassDecl = if (isList) {
            resolvedType.arguments.firstOrNull()?.type?.resolve()?.declaration as? KSClassDeclaration
        } else null
        val listElementIsEnum = listElementClassDecl?.classKind == ClassKind.ENUM_CLASS
        val listElementEnumValues = if (listElementIsEnum) {
            listElementClassDecl!!.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .map { it.simpleName.asString() }
                .toList()
        } else emptyList()

        // Data class (limit to 1 level — sub-params of a data class are not recursed further)
        val isDataClass = !isList && !isEnum && classDecl != null && Modifier.DATA in classDecl.modifiers
        val dataClassParams = if (isDataClass) collectConstructorParams(classDecl!!) else emptyList()

        // Sealed class / sealed interface
        val isSealed = !isList && !isEnum && !isDataClass &&
            classDecl != null && Modifier.SEALED in classDecl.modifiers
        val sealedSubtypes = if (isSealed) collectSealedSubtypes(classDecl!!) else emptyList()

        return ParamModel(
            name = name,
            label = label,
            kotlinType = kotlinType,
            defaultValue = defaultValue,
            options = options,
            description = description,
            isEnum = isEnum,
            enumValues = enumValues,
            isNullable = isNullable,
            isDataClass = isDataClass,
            dataClassParams = dataClassParams,
            isSealedClass = isSealed,
            sealedSubtypes = sealedSubtypes,
            isList = isList,
            listElementKotlinType = listElementKotlinType,
            listElementIsEnum = listElementIsEnum,
            listElementEnumValues = listElementEnumValues,
        )
    }

    /** Builds plain [ParamModel] entries for each primary constructor parameter of [decl]. */
    private fun collectConstructorParams(decl: KSClassDeclaration): List<ParamModel> =
        decl.primaryConstructor?.parameters?.mapNotNull { param ->
            val resolvedType = param.type.resolve()
            val paramName = param.name?.asString() ?: return@mapNotNull null
            val paramDecl = resolvedType.declaration
            val paramType = paramDecl.qualifiedName?.asString() ?: ""
            val paramClassDecl = paramDecl as? KSClassDeclaration
            val paramIsNullable = resolvedType.isMarkedNullable
            val paramIsEnum = paramClassDecl?.classKind == ClassKind.ENUM_CLASS
            val paramEnumValues = if (paramIsEnum) {
                paramClassDecl!!.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .filter { it.classKind == ClassKind.ENUM_ENTRY }
                    .map { it.simpleName.asString() }
                    .toList()
            } else emptyList()
            ParamModel(
                name = paramName,
                label = paramName,
                kotlinType = paramType,
                defaultValue = "",
                options = emptyList(),
                description = "",
                isEnum = paramIsEnum,
                enumValues = paramEnumValues,
                isNullable = paramIsNullable,
            )
        } ?: emptyList()

    /** Collects direct sealed subtypes and their constructor params. */
    private fun collectSealedSubtypes(decl: KSClassDeclaration): List<SealedSubtype> =
        decl.getSealedSubclasses().map { subDecl ->
            val isObject = subDecl.classKind == ClassKind.OBJECT
            SealedSubtype(
                name = subDecl.simpleName.asString(),
                qualifiedName = subDecl.qualifiedName?.asString() ?: "",
                isObject = isObject,
                params = if (isObject) emptyList() else collectConstructorParams(subDecl),
            )
        }.toList()

    /** Derives a stable Kotlin identifier for the synthetic PreviewGroup object for a @Preview group string. */
    private fun syntheticGroupObjectName(groupStr: String): String {
        val sanitized = groupStr.replace(Regex("[^A-Za-z0-9]"), "").ifEmpty { "Default" }
        return "GeneratedAndroidPreviewGroup_$sanitized"
    }

    private fun resolveGroupExpression(groupType: KSType?): Pair<String, String> {
        val declaration = groupType?.declaration ?: return "" to ""

        // Walk up the parent chain to collect simple names from outermost to this declaration.
        val parts = mutableListOf<String>()
        var current: KSDeclaration? = declaration
        while (current != null && current is KSClassDeclaration) {
            parts.add(0, current.simpleName.asString())
            current = current.parentDeclaration
        }

        if (parts.isEmpty()) return "" to ""

        val packageName = declaration.packageName.asString()
        val expression = parts.joinToString(".")
        val topLevelImport = if (packageName.isEmpty()) parts[0] else "$packageName.${parts[0]}"
        return expression to topLevelImport
    }
}
