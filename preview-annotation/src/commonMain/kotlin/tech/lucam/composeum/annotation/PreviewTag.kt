package tech.lucam.composeum.annotation

/** Marker interface for searchable preview tags shown in the browser UI. */
interface PreviewTag {
    val title: String
}

/** Simple runtime tag implementation used by the manual registry DSL and KDoc fallbacks. */
data class SimplePreviewTag(
    override val title: String,
) : PreviewTag
