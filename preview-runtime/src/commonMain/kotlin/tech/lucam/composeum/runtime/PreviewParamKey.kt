package tech.lucam.composeum.runtime

/** Type-safe identifier for a preview parameter stored in [PreviewParamState]. */
data class PreviewParamKey<T>(
    val name: String,
)

fun <T> previewParamKey(name: String): PreviewParamKey<T> = PreviewParamKey(name)
