package tech.lucam.composeum.annotation

/** Marker interface for sealed group hierarchies used to organise previews in the browser. */
interface PreviewGroup {
    val name: String
    val description: String get() = ""
}
