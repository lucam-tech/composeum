package tech.lucam.composeum.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import tech.lucam.composeum.runtime.config.ThemeOption
import tech.lucam.composeum.runtime.config.ThemeOptionDefaults

/** Minimal Material3 theme used as the browser's [browserWrapper]. */
@Composable
fun SampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: ThemeOption = ThemeOptionDefaults.Classic,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            darkColorScheme(primary = theme.primary, secondary = theme.secondary)
        } else {
            lightColorScheme(primary = theme.primary, secondary = theme.secondary)
        },
        content = content,
    )
}
