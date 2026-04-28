package tech.lucam.composeum.runtime.config

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** A selectable browser theme that overrides Material primary and secondary colors. */
@Immutable
data class ThemeOption(
    val id: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
)

/** Built-in browser themes shown in settings by default. */
object ThemeOptionDefaults {
    val Classic = ThemeOption(
        id = "classic",
        displayName = "Classic",
        primary = Color(0xFF6750A4),
        secondary = Color(0xFF625B71),
    )

    val Ocean = ThemeOption(
        id = "ocean",
        displayName = "Ocean",
        primary = Color(0xFF006C84),
        secondary = Color(0xFF4A6268),
    )

    val Forest = ThemeOption(
        id = "forest",
        displayName = "Forest",
        primary = Color(0xFF406836),
        secondary = Color(0xFF55624C),
    )

    val Sunset = ThemeOption(
        id = "sunset",
        displayName = "Sunset",
        primary = Color(0xFF9C4321),
        secondary = Color(0xFF77574E),
    )

    val all: List<ThemeOption> = listOf(Classic, Ocean, Forest, Sunset)
}

internal fun PreviewConfig.resolvedThemeOptions(): List<ThemeOption> {
    val merged = linkedMapOf<String, ThemeOption>()
    ThemeOptionDefaults.all.forEach { merged[it.id] = it }
    themeOptions.forEach { merged[it.id] = it }
    return merged.values.toList()
}
