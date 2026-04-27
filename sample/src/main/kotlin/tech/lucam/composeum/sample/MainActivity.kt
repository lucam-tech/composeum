package tech.lucam.composeum.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.runtime.CompositePreviewRegistry
import tech.lucam.composeum.runtime.config.BuiltInSettingId
import tech.lucam.composeum.runtime.config.GroupExpansionMode
import tech.lucam.composeum.runtime.config.SettingItem
import tech.lucam.composeum.runtime.config.previewConfig
import tech.lucam.composeum.runtime.ui.LocalIsDarkTheme
import tech.lucam.composeum.runtime.ui.ComposeumBrowserActivity
import tech.lucam.composeum.sample.AlertSeverity
import tech.lucam.composeum.sample.generated.GeneratedPreviewRegistry

/**
 * Sample entry point.  Subclasses [ComposeumBrowserActivity] and provides the
 * generated registry plus a config that exercises every customization option
 * the library supports.
 */
class MainActivity : ComposeumBrowserActivity() {

    override val registry = CompositePreviewRegistry(
        GeneratedPreviewRegistry,
        MyDslRegistry,
    )

    override val config = previewConfig {

        // ── Global display settings ───────────────────────────────────────────
        // These are the DSL defaults; override them to change the out-of-the-box
        // experience before the user touches the settings sheet.
        thumbnailColumns = 2          // default grid column count (runtime-overridable)
        showDescriptions = true       // show group descriptions in the group list
        showTags         = true       // show tag chips on thumbnail cards
        showParamPanel   = true       // show the param panel on the detail screen

        // ── groupExpansionMode ────────────────────────────────────────────────
        // INLINE expands leaf groups in-place to show a component list for quick
        // access.  SUBSCREEN (default) navigates to a full PreviewListScreen.
        // You can override this per group inside the groups { } block below.
        groupExpansionMode = GroupExpansionMode.INLINE

        // ── Custom top-bar action ─────────────────────────────────────────────
        // Adds an icon button before the settings icon in the top app bar.
        topBarAction(
            contentDescription = "About this sample",
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = { /* open an about dialog or sheet in a real app */ },
        )

        // ── Custom settings items ─────────────────────────────────────────────
        // Provide an ordered list to control which built-ins appear and inject
        // your own composables anywhere.  Omitting a BuiltInSettingId removes
        // that control entirely.  null (default) shows all built-ins.
        settingsItems = listOf(
            SettingItem.BuiltIn(BuiltInSettingId.THEME),
            SettingItem.BuiltIn(BuiltInSettingId.FONT_SCALE),
            SettingItem.BuiltIn(BuiltInSettingId.UI_SCALE),
            SettingItem.BuiltIn(BuiltInSettingId.THUMBNAIL_COLUMNS),
            SettingItem.BuiltIn(BuiltInSettingId.SHOW_DESCRIPTIONS),
            SettingItem.BuiltIn(BuiltInSettingId.SHOW_TAGS),
            SettingItem.BuiltIn(BuiltInSettingId.LOCALE),
            // Custom item injected between locale and reset.
            SettingItem.Custom("sample_about") {
                androidx.compose.material3.Text(
                    text = "Compose Preview — Sample App",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            SettingItem.BuiltIn(BuiltInSettingId.RESET),
        )

        // ── Source link ───────────────────────────────────────────────────────
        // When sourceBaseUrl is set, the detail screen shows an icon button that
        // opens the composable's source file in your browser (GitLab / GitHub).
        // Strip the machine-specific project root so only the repo-relative path
        // remains.  Remove both lines to use idea://open deep links instead
        // (works when Android Studio is attached via ADB on an emulator).
        //
        // sourceBaseUrl = "https://gitlab.com/org/repo/-/blob/main"
        // sourceStripPrefix = "/home/user/workspace/my-project"

        // ── Custom type field widget ──────────────────────────────────────────────
        // AlertSeverity is a plain class — KSP cannot introspect it as enum/data class/sealed,
        // so the param panel falls through to PreviewCustomTypeField. This registration
        // provides a chip-picker widget and the AlertSeverity.MEDIUM initial value.
        customTypeField<AlertSeverity>(initialValue = AlertSeverity.MEDIUM) { value, onValue ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AlertSeverity.ALL.forEach { sev ->
                    FilterChip(
                        selected = value.level == sev.level,
                        onClick = { onValue(sev) },
                        label = { Text(sev.label) },
                    )
                }
            }
        }

        // ── browserWrapper ────────────────────────────────────────────────────
        // Wraps the ENTIRE browser NavHost.  Use this to apply your app's theme
        // so that all previews render inside the correct MaterialTheme.
        browserWrapper { content ->
            val isDark = LocalIsDarkTheme.current
            SampleTheme(darkTheme = isDark) { content() }
        }

        // ── Global previewWrapper ─────────────────────────────────────────────
        // Applied to EVERY preview card across all groups unless a per-group
        // previewWrapper override is provided.  Here we add a slight tonal
        // elevation so each thumbnail sits visually above the grid background.
        previewWrapper { _, content ->
            Surface(
                shape        = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
            ) {
                content()
            }
        }

        // ── Per-group overrides ───────────────────────────────────────────────
        groups {

            // Screens — override the global INLINE mode back to SUBSCREEN so
            // full-screen layouts are always accessed via the grid view.
            group<SampleGroup.Screens> {
                expansionMode = GroupExpansionMode.SUBSCREEN
            }

            // Forms — groupWrapper: render the whole grid on a surface-variant
            // background so the form-control previews stand out.
            group<SampleGroup.Forms> {
                groupWrapper { _, content ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        content()
                    }
                }
                // (inherits the global previewWrapper — no override needed)
            }

            // Themed › Dark — groupWrapper: dark backdrop; previewWrapper:
            // replaces the global elevated surface with plain padding so the
            // dark background shows through between cards.
            group<SampleGroup.Themed.Dark> {
                groupWrapper { _, content ->
                    // Material3 dark-theme reference background colour (#1C1B1F)
                    Surface(color = Color(red = 28 / 255f, green = 27 / 255f, blue = 31 / 255f)) {
                        content()
                    }
                }
                previewWrapper { preview, content ->
                    // Override global wrapper: no elevation card, just spacing.
                    Box(modifier = Modifier.padding(8.dp)) { content() }
                }
            }

            // Themed › Accented — groupWrapper: primary-container tint to check
            // contrast of components against a brand accent colour.
            group<SampleGroup.Themed.Accented> {
                groupWrapper { _, content ->
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        content()
                    }
                }
                // (inherits the global previewWrapper)
            }

            // Showcase — thumbnailColumns: force single-column wide layout;
            // previewWrapper: bordered card so each preview has a clear boundary.
            group<SampleGroup.Showcase> {
                thumbnailColumns = 1
                previewWrapper { _, content ->
                    Surface(
                        shape    = MaterialTheme.shapes.medium,
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) { content() }
                    }
                }
            }
        }
    }
}
