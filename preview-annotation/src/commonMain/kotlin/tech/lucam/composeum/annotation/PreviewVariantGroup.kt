package tech.lucam.composeum.annotation

/**
 * Marker interface for preview flavor families.
 *
 * Use a dedicated object type per preview family so multiple concrete previews can be grouped
 * into one catalog entry while still remaining type-safe at compile time.
 */
interface PreviewVariantGroup
