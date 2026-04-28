package tech.lucam.composeum.runtime.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.runtime.config.BuiltInSettingId
import tech.lucam.composeum.runtime.config.LocaleOption
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.SettingItem
import tech.lucam.composeum.runtime.config.ThemeOption
import tech.lucam.composeum.runtime.config.resolvedThemeOptions
import tech.lucam.composeum.runtime.store.RuntimeSettings
import tech.lucam.composeum.runtime.store.SettingsStorage
import tech.lucam.composeum.runtime.store.ThemeOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val defaultLocaleOptions = listOf(
    LocaleOption(tag = "system", displayName = "System"),
    LocaleOption(tag = "en",     displayName = "English"),
    LocaleOption(tag = "de",     displayName = "Deutsch"),
    LocaleOption(tag = "fr",     displayName = "Français"),
    LocaleOption(tag = "es",     displayName = "Español"),
    LocaleOption(tag = "ja",     displayName = "日本語"),
    LocaleOption(tag = "ar",     displayName = "العربية"),
)

private val defaultSettingsItems: List<SettingItem> = listOf(
    SettingItem.BuiltIn(BuiltInSettingId.THEME),
    SettingItem.BuiltIn(BuiltInSettingId.FONT_SCALE),
    SettingItem.BuiltIn(BuiltInSettingId.UI_SCALE),
    SettingItem.BuiltIn(BuiltInSettingId.THUMBNAIL_COLUMNS),
    SettingItem.BuiltIn(BuiltInSettingId.SHOW_DESCRIPTIONS),
    SettingItem.BuiltIn(BuiltInSettingId.SHOW_TAGS),
    SettingItem.BuiltIn(BuiltInSettingId.LOCALE),
    SettingItem.BuiltIn(BuiltInSettingId.RESET),
)

/**
 * Settings bottom sheet content.
 *
 * Reads current values from [LocalResolvedSettings] and [LocalSettingsStorage].
 * Each control calls [SettingsStorage.update] on change.
 * The reset button calls [SettingsStorage.reset].
 *
 * When [config.settingsItems][PreviewConfig.settingsItems] is non-null the sheet renders exactly
 * those items in the given order. Omit a [BuiltInSettingId] to hide that control; insert
 * [SettingItem.Custom] entries to add your own composables anywhere in the list.
 *
 * @param config   The browser config used as the baseline for default values.
 * @param modifier Modifier applied to the root column.
 */
@Composable
fun SettingsSheet(
    config: PreviewConfig,
    modifier: Modifier = Modifier,
) {
    val storage = LocalSettingsStorage.current
    val scope = rememberCoroutineScope()
    val runtimeSettings by storage.settings.collectAsState(initial = RuntimeSettings())

    val items = config.settingsItems ?: defaultSettingsItems

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
        )

        items.forEachIndexed { index, item ->
            when (item) {
                is SettingItem.BuiltIn -> BuiltInSetting(
                    id = item.id,
                    config = config,
                    runtimeSettings = runtimeSettings,
                    storage = storage,
                    scope = scope,
                )
                is SettingItem.Custom -> item.content()
            }
            if (index < items.lastIndex) {
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Built-in setting dispatcher ───────────────────────────────────────────────

@Composable
private fun BuiltInSetting(
    id: String,
    config: PreviewConfig,
    runtimeSettings: RuntimeSettings,
    storage: SettingsStorage,
    scope: CoroutineScope,
) {
    when (id) {
        BuiltInSettingId.THEME -> ThemeSetting(config, runtimeSettings, storage, scope)
        BuiltInSettingId.FONT_SCALE -> FontScaleSetting(config, runtimeSettings, storage, scope)
        BuiltInSettingId.UI_SCALE -> UiScaleSetting(config, runtimeSettings, storage, scope)
        BuiltInSettingId.THUMBNAIL_COLUMNS -> ThumbnailColumnsSetting(config, runtimeSettings, storage, scope)
        BuiltInSettingId.SHOW_DESCRIPTIONS -> ShowDescriptionsSetting(config, runtimeSettings, storage, scope)
        BuiltInSettingId.SHOW_TAGS -> ShowTagsSetting(config, runtimeSettings, storage, scope)
        BuiltInSettingId.LOCALE -> LocaleSetting(runtimeSettings, storage, scope,
            config.localeOptions ?: defaultLocaleOptions)
        BuiltInSettingId.RESET -> ResetSetting(storage, scope)
        // Unknown IDs are silently ignored to allow forward compatibility.
    }
}

// ── Individual built-in settings ──────────────────────────────────────────────

@Composable
private fun ThemeSetting(
    config: PreviewConfig,
    runtimeSettings: RuntimeSettings,
    storage: SettingsStorage,
    scope: CoroutineScope,
) {
    val themeOptions = config.resolvedThemeOptions()
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedTheme = resolveSelectedTheme(config, runtimeSettings, themeOptions)

    SettingLabel("Theme mode")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ThemeOverride.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = runtimeSettings.themeOverride == option,
                onClick = {
                    scope.launch { storage.update { copy(themeOverride = option) } }
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeOverride.entries.size),
            ) {
                Text(
                    when (option) {
                        ThemeOverride.LIGHT -> "Light"
                        ThemeOverride.DARK -> "Dark"
                        ThemeOverride.SYSTEM -> "System"
                    },
                )
            }
        }
    }

    SettingLabel("Theme palette")
    ExposedDropdownMenuBox(
        expanded = menuExpanded,
        onExpandedChange = { menuExpanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedTheme.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = "Theme dropdown" },
        )
        ExposedDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            themeOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        scope.launch {
                            storage.update {
                                copy(themeId = option.id)
                            }
                        }
                        menuExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FontScaleSetting(
    config: PreviewConfig,
    runtimeSettings: RuntimeSettings,
    storage: SettingsStorage,
    scope: CoroutineScope,
) {
    val resolved = runtimeSettings.fontScale ?: config.fontScale
    var local by remember(resolved) { mutableStateOf(resolved) }

    SettingLabel("Font scale: ${local.format1dp()}×")
    Slider(
        value = local,
        onValueChange = { newValue ->
            local = newValue
            scope.launch { storage.update { copy(fontScale = newValue) } }
        },
        valueRange = 0.5f..2.0f,
        steps = 14,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Font scale slider" },
    )
}

@Composable
private fun UiScaleSetting(
    config: PreviewConfig,
    runtimeSettings: RuntimeSettings,
    storage: SettingsStorage,
    scope: CoroutineScope,
) {
    val resolved = runtimeSettings.uiScale ?: config.uiScale
    var local by remember(resolved) { mutableStateOf(resolved) }

    SettingLabel("UI scale: ${local.format1dp()}×")
    Slider(
        value = local,
        onValueChange = { newValue ->
            local = newValue
            scope.launch { storage.update { copy(uiScale = newValue) } }
        },
        valueRange = 0.5f..2.0f,
        steps = 14,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "UI scale slider" },
    )
}

@Composable
private fun ThumbnailColumnsSetting(
    config: PreviewConfig,
    runtimeSettings: RuntimeSettings,
    storage: SettingsStorage,
    scope: CoroutineScope,
) {
    val resolved = runtimeSettings.thumbnailColumns ?: config.thumbnailColumns
    SettingLabel("Thumbnail columns")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FilledTonalIconButton(
            onClick = {
                val next = (resolved - 1).coerceAtLeast(1)
                scope.launch { storage.update { copy(thumbnailColumns = next) } }
            },
            enabled = resolved > 1,
            modifier = Modifier.semantics { contentDescription = "Decrease thumbnail columns" },
        ) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = "$resolved",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Thumbnail columns value" },
        )
        Spacer(Modifier.width(16.dp))
        FilledTonalIconButton(
            onClick = {
                val next = (resolved + 1).coerceAtMost(4)
                scope.launch { storage.update { copy(thumbnailColumns = next) } }
            },
            enabled = resolved < 4,
            modifier = Modifier.semantics { contentDescription = "Increase thumbnail columns" },
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ShowDescriptionsSetting(
    config: PreviewConfig,
    runtimeSettings: RuntimeSettings,
    storage: SettingsStorage,
    scope: CoroutineScope,
) {
    val resolved = runtimeSettings.showDescriptions ?: config.showDescriptions
    SettingRow(
        label = "Show descriptions",
        control = {
            Switch(
                checked = resolved,
                onCheckedChange = { checked ->
                    scope.launch { storage.update { copy(showDescriptions = checked) } }
                },
                modifier = Modifier.semantics { contentDescription = "Show descriptions toggle" },
            )
        },
    )
}

@Composable
private fun ShowTagsSetting(
    config: PreviewConfig,
    runtimeSettings: RuntimeSettings,
    storage: SettingsStorage,
    scope: CoroutineScope,
) {
    val resolved = runtimeSettings.showTags ?: config.showTags
    SettingRow(
        label = "Show tags",
        control = {
            Switch(
                checked = resolved,
                onCheckedChange = { checked ->
                    scope.launch { storage.update { copy(showTags = checked) } }
                },
                modifier = Modifier.semantics { contentDescription = "Show tags toggle" },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocaleSetting(
    runtimeSettings: RuntimeSettings,
    storage: SettingsStorage,
    scope: CoroutineScope,
    options: List<LocaleOption>,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedTag = runtimeSettings.locale ?: "system"
    val selectedDisplay = options.firstOrNull { it.tag == selectedTag }?.displayName ?: selectedTag

    SettingLabel("Locale")
    ExposedDropdownMenuBox(
        expanded = menuExpanded,
        onExpandedChange = { menuExpanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedDisplay,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = "Locale dropdown" },
        )
        ExposedDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        scope.launch {
                            storage.update {
                                copy(locale = if (option.tag == "system") null else option.tag)
                            }
                        }
                        menuExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ResetSetting(
    storage: SettingsStorage,
    scope: CoroutineScope,
) {
    Button(
        onClick = { scope.launch { storage.reset() } },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Reset all settings")
    }
}

// ── Shared layout helpers ─────────────────────────────────────────────────────

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        control()
    }
}

private fun Float.format1dp(): String {
    val whole = toInt()
    val dec = kotlin.math.abs(((this - whole) * 10).toInt())
    return "$whole.$dec"
}

private fun resolveSelectedTheme(
    config: PreviewConfig,
    runtimeSettings: RuntimeSettings,
    options: List<ThemeOption>,
): ThemeOption {
    val selectedThemeId = runtimeSettings.themeId ?: config.defaultThemeId
    return options.firstOrNull { it.id == selectedThemeId } ?: options.first()
}
