package tech.lucam.composeum.ksp.model

/**
 * Conservative parser for KDoc exposed via KSP's `docString`.
 *
 * Supported fallbacks:
 * - first summary paragraph -> preview description
 * - `@param name ...` -> preview parameter description
 * - `@tag foo` / `@tags foo, bar` -> preview tags
 */
internal object KDocParser {

    data class ParsedKDoc(
        val summary: String,
        val paramDescriptions: Map<String, String>,
        val tags: List<String>,
    )

    fun parse(docString: String?): ParsedKDoc {
        val lines = docString
            ?.lineSequence()
            ?.map { raw ->
                raw.trim()
                    .removePrefix("/**")
                    .removeSuffix("*/")
                    .removePrefix("*")
                    .trim()
            }
            ?.toList()
            ?: emptyList()

        if (lines.isEmpty()) {
            return ParsedKDoc(summary = "", paramDescriptions = emptyMap(), tags = emptyList())
        }

        return ParsedKDoc(
            summary = buildSummary(lines),
            paramDescriptions = buildParamDescriptions(lines),
            tags = buildTags(lines),
        )
    }

    private fun buildSummary(lines: List<String>): String {
        val summary = mutableListOf<String>()
        for (line in lines) {
            when {
                line.isBlank() && summary.isNotEmpty() -> break
                line.isBlank() -> continue
                line.startsWith("@") -> break
                else -> summary += line
            }
        }
        return summary.joinToString(" ").normalizeWhitespace()
    }

    private fun buildParamDescriptions(lines: List<String>): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var currentParam: String? = null
        val buffer = mutableListOf<String>()

        fun flush() {
            val name = currentParam ?: return
            val text = buffer.joinToString(" ").normalizeWhitespace()
            if (text.isNotEmpty()) result[name] = text
            currentParam = null
            buffer.clear()
        }

        for (line in lines) {
            if (line.startsWith("@param ")) {
                flush()
                val remainder = line.removePrefix("@param ").trim()
                val name = remainder.substringBefore(' ').trim().trim('[', ']')
                val description = remainder.substringAfter(' ', "").trim()
                if (name.isNotEmpty()) {
                    currentParam = name
                    if (description.isNotEmpty()) buffer += description
                }
                continue
            }

            if (currentParam != null) {
                when {
                    line.isBlank() -> flush()
                    line.startsWith("@") -> flush()
                    else -> buffer += line
                }
            }
        }

        flush()
        return result
    }

    private fun buildTags(lines: List<String>): List<String> =
        lines.asSequence()
            .flatMap { line ->
                when {
                    line.startsWith("@tag ") -> sequenceOf(line.removePrefix("@tag ").trim())
                    line.startsWith("@tags ") -> line.removePrefix("@tags ")
                        .split(',')
                        .asSequence()
                        .map { it.trim() }
                    else -> emptySequence()
                }
            }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

    private fun String.normalizeWhitespace(): String =
        replace(Regex("\\s+"), " ").trim()
}
