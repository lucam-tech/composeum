package tech.lucam.composeum.runtime.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.PreviewWrapper
import tech.lucam.composeum.runtime.ui.component.LocalResolvedSettings
import tech.lucam.composeum.runtime.ui.component.PreviewThumbnailCard

/**
 * Displays a grid of [PreviewThumbnailCard]s for a single group.
 *
 * The group is identified by [groupKey] (the qualified class name of the
 * [tech.lucam.composeum.annotation.PreviewGroup] object). The grid column count
 * and wrappers are resolved from [config] group overrides, falling back to global
 * config and then to [tech.lucam.composeum.runtime.store.ResolvedSettings].
 *
 * @param groupKey        Qualified class name of the target group.
 * @param registry        Source of all preview entries.
 * @param config          Browser configuration (wrappers, overrides).
 * @param onEntrySelected Called with [tech.lucam.composeum.runtime.PreviewEntry.key] when a card is tapped.
 * @param modifier        Modifier applied to the root box.
 */
@Composable
fun PreviewListScreen(
    groupKey: String,
    registry: PreviewRegistry,
    config: PreviewConfig,
    onEntrySelected: (entryKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = LocalResolvedSettings.current

    val entries = remember(registry.entries, groupKey) {
        registry.entries.filter { entry ->
            (entry.group::class.qualifiedName ?: entry.group::class.simpleName ?: "") == groupKey
        }
    }

    val group = entries.firstOrNull()?.group
    val groupConfig = group?.let { config.groupOverrides[it::class] }

    val thumbnailColumns = groupConfig?.thumbnailColumns ?: settings.thumbnailColumns
    val effectiveGroupWrapper = groupConfig?.groupWrapper ?: config.groupWrapper
    val effectivePreviewWrapper: PreviewWrapper? = groupConfig?.previewWrapper ?: config.previewWrapper

    val gridContent: @Composable () -> Unit = {
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No previews in this group.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(thumbnailColumns.coerceAtLeast(1)),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(entries, key = { it.key }) { entry ->
                    PreviewThumbnailCard(
                        entry = entry,
                        previewWrapper = effectivePreviewWrapper,
                        showTags = settings.showTags,
                        onClick = { onEntrySelected(entry.key) },
                    )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (effectiveGroupWrapper != null && group != null) {
            effectiveGroupWrapper(group, gridContent)
        } else {
            gridContent()
        }
    }
}
