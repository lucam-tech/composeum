package tech.lucam.composeum.runtime

internal data class PreviewFamily(
    val key: String,
    val entries: List<PreviewEntry>,
) {
    init {
        require(entries.isNotEmpty()) { "PreviewFamily requires at least one entry." }
    }

    val defaultEntry: PreviewEntry = entries.firstOrNull { it.isDefaultVariant } ?: entries.first()
}

internal fun PreviewEntry.familyKey(): String =
    variantGroup?.variantGroupKey() ?: key

internal fun List<PreviewEntry>.groupIntoFamilies(): List<PreviewFamily> {
    val families = linkedMapOf<String, MutableList<PreviewEntry>>()
    for (entry in this) {
        families.getOrPut(entry.familyKey()) { mutableListOf() }.add(entry)
    }
    return families.map { (familyKey, entries) -> PreviewFamily(key = familyKey, entries = entries.toList()) }
}

internal fun PreviewRegistry.families(): List<PreviewFamily> = entries.groupIntoFamilies()
