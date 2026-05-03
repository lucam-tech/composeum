package tech.lucam.composeum.ksp.model

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import java.io.File

internal object FunctionDefaultParser {
    private val fileCache = mutableMapOf<String, String>()

    fun parse(function: KSFunctionDeclaration): Map<String, String> {
        val filePath = function.containingFile?.filePath ?: return emptyMap()
        val fileText = fileCache.getOrPut(filePath) { File(filePath).readText() }
        val lineNumber = (function.location as? FileLocation)?.lineNumber ?: return emptyMap()
        val rawDefaults = extractRawDefaults(fileText, function.simpleName.asString(), lineNumber)
        if (rawDefaults.isEmpty()) return emptyMap()
        return function.parameters.mapNotNull { param ->
            val name = param.name?.asString() ?: return@mapNotNull null
            val raw = rawDefaults[name] ?: return@mapNotNull null
            name to normalizeDefault(param, raw)
        }.toMap()
    }

    private fun extractRawDefaults(
        fileText: String,
        functionName: String,
        lineNumber: Int,
    ): Map<String, String> {
        val startOffset = lineStartOffset(fileText, lineNumber).coerceAtLeast(0)
        val searchStart = (startOffset - 512).coerceAtLeast(0)
        val signatureStart = fileText.indexOf("fun $functionName", startIndex = searchStart)
            .takeIf { it >= 0 } ?: return emptyMap()
        val openParen = fileText.indexOf('(', startIndex = signatureStart)
            .takeIf { it >= 0 } ?: return emptyMap()
        val closeParen = findMatchingDelimiter(fileText, openParen, '(', ')') ?: return emptyMap()
        val paramsText = fileText.substring(openParen + 1, closeParen)
        return splitTopLevel(paramsText, ',').mapNotNull { paramText ->
            val equalsIndex = topLevelIndexOf(paramText, '=')
            if (equalsIndex < 0) return@mapNotNull null
            val left = paramText.substring(0, equalsIndex)
            val right = paramText.substring(equalsIndex + 1).trim()
            val name = left.substringBefore(':').trim()
                .split(Regex("\\s+"))
                .lastOrNull()
                ?.removePrefix("@")
                ?: return@mapNotNull null
            name to right
        }.toMap()
    }

    private fun normalizeDefault(param: KSValueParameter, raw: String): String {
        val resolvedType = param.type.resolve()
        val typeName = resolvedType.declaration.qualifiedName?.asString() ?: resolvedType.toString()
        val classDecl = resolvedType.declaration as? KSClassDeclaration
        val trimmed = raw.trim()

        if (trimmed == "null") return "null"
        if (classDecl != null && Modifier.DATA in classDecl.modifiers) return ""

        return when {
            classDecl?.classKind == ClassKind.ENUM_CLASS ->
                trimmed.substringAfterLast('.').substringBefore('(').trim()
            typeName == "kotlin.String" -> parseStringLiteral(trimmed) ?: trimmed
            typeName == "kotlin.Boolean" -> trimmed
            typeName == "kotlin.Int" -> trimmed.removeSuffix("_")
            typeName == "kotlin.Long" -> trimmed.removeSuffix("L").removeSuffix("l")
            typeName == "kotlin.Float" -> trimmed.removeSuffix("f").removeSuffix("F")
            typeName == "kotlin.Double" -> trimmed.removeSuffix("d").removeSuffix("D")
            typeName == "androidx.compose.ui.unit.Dp" -> trimmed.substringBefore(".dp").trim()
            typeName == "androidx.compose.ui.unit.TextUnit" ->
                trimmed.substringBefore(".sp").substringBefore(".em").trim()
            typeName == "androidx.compose.ui.graphics.Color" -> normalizeColor(trimmed)
            isSealedClass(classDecl) -> trimmed.substringAfterLast('.').substringBefore('(').trim()
            typeName == "kotlin.collections.List" -> normalizeList(resolvedType, trimmed)
            else -> ""
        }
    }

    private fun normalizeColor(raw: String): String {
        val inner = raw.substringAfter("Color(", missingDelimiterValue = raw)
            .substringBeforeLast(")")
            .trim()
        return when {
            inner.startsWith("0x") || inner.startsWith("0X") ->
                inner.removePrefix("0x").removePrefix("0X").toLong(16).toString()
            else -> inner.toLongOrNull()?.toString().orEmpty()
        }
    }

    private fun normalizeList(type: com.google.devtools.ksp.symbol.KSType, raw: String): String {
        val elementType = type.arguments.firstOrNull()?.type?.resolve() ?: return ""
        val elementDecl = elementType.declaration as? KSClassDeclaration
        val items = raw.substringAfter('(', missingDelimiterValue = "")
            .substringBeforeLast(")", "")
            .let { splitTopLevel(it, ',') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return items.joinToString("|") { item ->
            when {
                elementDecl?.classKind == ClassKind.ENUM_CLASS ->
                    item.substringAfterLast('.').trim()
                elementType.declaration.qualifiedName?.asString() == "kotlin.String" ->
                    parseStringLiteral(item) ?: item
                elementType.declaration.qualifiedName?.asString() == "kotlin.Long" ->
                    item.removeSuffix("L").removeSuffix("l")
                elementType.declaration.qualifiedName?.asString() == "kotlin.Float" ->
                    item.removeSuffix("f").removeSuffix("F")
                elementType.declaration.qualifiedName?.asString() == "kotlin.Double" ->
                    item.removeSuffix("d").removeSuffix("D")
                else -> item
            }
        }
    }

    private fun parseStringLiteral(raw: String): String? {
        if (raw.length < 2 || raw.first() != '"' || raw.last() != '"') return null
        return raw.substring(1, raw.lastIndex)
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\\\", "\\")
    }

    private fun isSealedClass(classDecl: KSClassDeclaration?): Boolean =
        classDecl != null && Modifier.SEALED in classDecl.modifiers

    private fun lineStartOffset(text: String, lineNumber: Int): Int {
        if (lineNumber <= 1) return 0
        var currentLine = 1
        var index = 0
        while (index < text.length && currentLine < lineNumber) {
            if (text[index] == '\n') currentLine++
            index++
        }
        return index
    }

    private fun splitTopLevel(text: String, delimiter: Char): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var start = 0
        var parenDepth = 0
        var bracketDepth = 0
        var braceDepth = 0
        var angleDepth = 0
        var inString = false
        var escaped = false

        text.forEachIndexed { index, char ->
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                return@forEachIndexed
            }

            when (char) {
                '"' -> inString = true
                '(' -> parenDepth++
                ')' -> parenDepth--
                '[' -> bracketDepth++
                ']' -> bracketDepth--
                '{' -> braceDepth++
                '}' -> braceDepth--
                '<' -> angleDepth++
                '>' -> angleDepth--
                delimiter -> if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0 && angleDepth == 0) {
                    result += text.substring(start, index)
                    start = index + 1
                }
            }
        }

        result += text.substring(start)
        return result
    }

    private fun topLevelIndexOf(text: String, target: Char): Int {
        var parenDepth = 0
        var bracketDepth = 0
        var braceDepth = 0
        var angleDepth = 0
        var inString = false
        var escaped = false

        text.forEachIndexed { index, char ->
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                return@forEachIndexed
            }

            when (char) {
                '"' -> inString = true
                '(' -> parenDepth++
                ')' -> parenDepth--
                '[' -> bracketDepth++
                ']' -> bracketDepth--
                '{' -> braceDepth++
                '}' -> braceDepth--
                '<' -> angleDepth++
                '>' -> angleDepth--
                target -> if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0 && angleDepth == 0) {
                    return index
                }
            }
        }

        return -1
    }

    private fun findMatchingDelimiter(
        text: String,
        openIndex: Int,
        open: Char,
        close: Char,
    ): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in openIndex until text.length) {
            val char = text[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }
}
