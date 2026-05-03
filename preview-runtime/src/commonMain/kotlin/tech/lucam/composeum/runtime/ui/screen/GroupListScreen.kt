package tech.lucam.composeum.runtime.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewFamily
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.GroupExpansionMode
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.PreviewWrapper
import tech.lucam.composeum.runtime.families
import tech.lucam.composeum.runtime.familyKey
import tech.lucam.composeum.runtime.tagKey
import tech.lucam.composeum.runtime.ui.component.LocalResolvedSettings
import tech.lucam.composeum.runtime.ui.component.LocalRuntimeSettings
import tech.lucam.composeum.runtime.ui.component.LocalSettingsStorage
import tech.lucam.composeum.runtime.ui.component.PreviewThumbnailCard

/**
 * Displays a searchable, expandable tree of preview groups.
 *
 * Leaf groups whose resolved [GroupExpansionMode] is [GroupExpansionMode.INLINE] expand
 * in-place to show a quick-access component list; tapping a component navigates directly
 * to the detail screen. A "See all" row at the bottom of the expanded list navigates to
 * the full [PreviewListScreen].
 *
 * Groups with [GroupExpansionMode.SUBSCREEN] (the default) navigate to [PreviewListScreen]
 * when tapped, which is the existing behaviour.
 *
 * @param registry         Source of all [tech.lucam.composeum.runtime.PreviewEntry] instances.
 * @param config           Browser configuration; controls per-group expansion mode.
 * @param onGroupSelected  Called with the group key when the user taps a leaf in SUBSCREEN mode
 *                         or presses "See all" from an INLINE-expanded group.
 * @param onEntrySelected  Called with the entry key when the user taps a component inside an
 *                         inline-expanded group. Required when any group uses INLINE mode.
 * @param modifier         Modifier applied to the root column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    registry: PreviewRegistry,
    config: PreviewConfig = PreviewConfig(),
    onGroupSelected: (groupKey: String) -> Unit,
    onEntrySelected: (entryKey: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val settings = LocalResolvedSettings.current
    val runtimeSettings = LocalRuntimeSettings.current
    val storage = LocalSettingsStorage.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val families = remember(registry.entries) { registry.families() }
    val rootNodes = remember(families) {
        buildGroupTree(families.map { it.defaultEntry })
    }
    val (rootPreviewNodes, groupedRootNodes) = remember(rootNodes) {
        rootNodes.partition { it.isLeaf && it.name.isEmpty() && it.description.isEmpty() }
    }

    // All unique tags from the registry, sorted alphabetically.
    val allTags = remember(registry.entries) {
        registry.entries
            .flatMap(PreviewEntry::tags)
            .distinctBy { it.tagKey() }
            .sortedBy { it.title.lowercase() }
    }
    var selectedTags by remember { mutableStateOf(emptySet<String>()) }

    // Tracks which non-leaf tree nodes are expanded (roots start expanded).
    var expandedKeys by remember(rootNodes, runtimeSettings.expandedGroupKeys) {
        mutableStateOf(runtimeSettings.expandedGroupKeys?.toSet() ?: rootNodes.map { it.key }
            .toSet())
    }
    var expandedKeysDirty by remember { mutableStateOf(false) }
    // Tracks which leaf nodes are expanded inline (all start collapsed).
    var inlineExpandedKeys by remember(runtimeSettings.inlineExpandedGroupKeys) {
        mutableStateOf(runtimeSettings.inlineExpandedGroupKeys?.toSet() ?: emptySet())
    }
    var inlineExpandedKeysDirty by remember { mutableStateOf(false) }
    val favoriteFamilyKeys = remember(runtimeSettings.favoriteFamilyKeys) {
        runtimeSettings.favoriteFamilyKeys.toSet()
    }
    val recentFamilyKeys = runtimeSettings.recentFamilyKeys
    val hasFlavoredFamilies = remember(families) { families.any { it.entries.size > 1 } }
    var favoritesOnly by remember { mutableStateOf(false) }
    var flavoredOnly by remember { mutableStateOf(false) }
    var favoritesExpanded by remember(runtimeSettings.favoritesExpanded) {
        mutableStateOf(runtimeSettings.favoritesExpanded ?: true)
    }
    var recentExpanded by remember(runtimeSettings.recentExpanded) {
        mutableStateOf(runtimeSettings.recentExpanded ?: true)
    }

    LaunchedEffect(expandedKeys, expandedKeysDirty) {
        if (!expandedKeysDirty) return@LaunchedEffect
        storage.update { copy(expandedGroupKeys = expandedKeys.toList()) }
    }
    LaunchedEffect(inlineExpandedKeys, inlineExpandedKeysDirty) {
        if (!inlineExpandedKeysDirty) return@LaunchedEffect
        storage.update { copy(inlineExpandedGroupKeys = inlineExpandedKeys.toList()) }
    }

    val favoriteFamilies = remember(families, favoriteFamilyKeys) {
        families.filter { it.key in favoriteFamilyKeys }
    }
    val recentFamilies = remember(families, recentFamilyKeys) {
        recentFamilyKeys.mapNotNull { key -> families.firstOrNull { it.key == key } }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search previews…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Text("×", style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else null,
            singleLine = true,
        )

        if (allTags.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(allTags, key = { it.tagKey() }) { tag ->
                    FilterChip(
                        selected = tag.tagKey() in selectedTags,
                        onClick = {
                            val tagKey = tag.tagKey()
                            selectedTags = if (tagKey in selectedTags) selectedTags - tagKey
                            else selectedTags + tagKey
                        },
                        label = { Text(tag.title) },
                    )
                }
            }
        }

        if (favoriteFamilies.isNotEmpty() || hasFlavoredFamilies) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (favoriteFamilies.isNotEmpty()) {
                    item {
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            label = { Text("Favorites") },
                        )
                    }
                }
                if (hasFlavoredFamilies) {
                    item {
                        FilterChip(
                            selected = flavoredOnly,
                            onClick = { flavoredOnly = !flavoredOnly },
                            label = { Text("Flavored") },
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        val isFiltered =
            query.isNotBlank() || selectedTags.isNotEmpty() || favoritesOnly || flavoredOnly

        if (!isFiltered) {
            LazyColumn {
                if (favoriteFamilies.isNotEmpty()) {
                    item(key = "favorites_header") {
                        SectionHeader(
                            title = "Favorites",
                            expanded = favoritesExpanded,
                            onToggle = {
                                favoritesExpanded = !favoritesExpanded
                                scope.launch {
                                    storage.update { copy(favoritesExpanded = favoritesExpanded) }
                                }
                            },
                        )
                    }
                    if (favoritesExpanded) {
                        entryGridItem(
                            nodeKey = "favorites",
                            entries = favoriteFamilies.map(PreviewFamily::defaultEntry),
                            config = config,
                            depth = 0,
                            onEntrySelected = onEntrySelected,
                        )
                    }
                }
                if (recentFamilies.isNotEmpty()) {
                    item(key = "recents_header") {
                        SectionHeader(
                            title = "Recent",
                            expanded = recentExpanded,
                            onToggle = {
                                recentExpanded = !recentExpanded
                                scope.launch {
                                    storage.update { copy(recentExpanded = recentExpanded) }
                                }
                            },
                        )
                    }
                    if (recentExpanded) {
                        entryGridItem(
                            nodeKey = "recent",
                            entries = recentFamilies.map(PreviewFamily::defaultEntry),
                            config = config,
                            depth = 0,
                            onEntrySelected = onEntrySelected,
                        )
                    }
                }
                rootPreviewNodes.forEach { node ->
                    entryGridItem(node.key, node.entries, config, 0, onEntrySelected)
                }
                treeItems(
                    nodes = groupedRootNodes,
                    expandedKeys = expandedKeys,
                    inlineExpandedKeys = inlineExpandedKeys,
                    onToggle = { key ->
                        expandedKeysDirty = true
                        expandedKeys = if (key in expandedKeys) expandedKeys - key
                        else expandedKeys + key
                    },
                    onInlineToggle = { key ->
                        inlineExpandedKeysDirty = true
                        inlineExpandedKeys = if (key in inlineExpandedKeys) inlineExpandedKeys - key
                        else inlineExpandedKeys + key
                    },
                    onLeafClick = onGroupSelected,
                    onEntrySelected = onEntrySelected,
                    config = config,
                    showDescriptions = settings.showDescriptions,
                    depth = 0,
                    resolveMode = { node -> resolveExpansionMode(node, config) },
                )
            }
        } else {
            val filtered = remember(
                query,
                selectedTags,
                rootNodes,
                favoriteFamilyKeys,
                favoritesOnly,
                flavoredOnly
            ) {
                filterNodes(
                    nodes = rootNodes,
                    query = query,
                    selectedTags = selectedTags,
                    favoriteFamilyKeys = favoriteFamilyKeys,
                    favoritesOnly = favoritesOnly,
                    flavoredOnly = flavoredOnly,
                )
            }
            val (filteredRootPreviewNodes, filteredGroupedNodes) = remember(filtered) {
                filtered.partition { it.isLeaf && it.name.isEmpty() && it.description.isEmpty() }
            }
            if (filteredRootPreviewNodes.isEmpty() && filteredGroupedNodes.isEmpty()) {
                Text(
                    text = "No previews match your search.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn {
                    filteredRootPreviewNodes.forEach { node ->
                        entryGridItem(node.key, node.entries, config, 0, onEntrySelected)
                    }
                    searchItems(
                        nodes = filteredGroupedNodes,
                        inlineExpandedKeys = inlineExpandedKeys,
                        onInlineToggle = { key ->
                            inlineExpandedKeysDirty = true
                            inlineExpandedKeys =
                                if (key in inlineExpandedKeys) inlineExpandedKeys - key
                                else inlineExpandedKeys + key
                        },
                        onLeafClick = onGroupSelected,
                        onEntrySelected = onEntrySelected,
                        config = config,
                        showDescriptions = settings.showDescriptions,
                        resolveMode = { node -> resolveExpansionMode(node, config) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowDown
            else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
        )
    }
}

// ── Lazy list builders ────────────────────────────────────────────────────────

private fun LazyListScope.treeItems(
    nodes: List<GroupNode>,
    expandedKeys: Set<String>,
    inlineExpandedKeys: Set<String>,
    onToggle: (String) -> Unit,
    onInlineToggle: (String) -> Unit,
    onLeafClick: (String) -> Unit,
    onEntrySelected: (String) -> Unit,
    config: PreviewConfig,
    showDescriptions: Boolean,
    depth: Int,
    resolveMode: (GroupNode) -> GroupExpansionMode,
) {
    for (node in nodes) {
        val mode = resolveMode(node)
        val isExpanded =
            if (node.isLeaf) node.key in inlineExpandedKeys else node.key in expandedKeys

        item(key = node.key) {
            GroupListItem(
                node = node,
                isExpanded = isExpanded,
                isLeaf = node.isLeaf,
                expansionMode = mode,
                depth = depth,
                showDescriptions = showDescriptions,
                onClick = {
                    when {
                        !node.isLeaf -> onToggle(node.key)
                        mode == GroupExpansionMode.INLINE -> onInlineToggle(node.key)
                        else -> onLeafClick(node.key)
                    }
                },
            )
        }

        if (!node.isLeaf && node.key in expandedKeys) {
            treeItems(
                nodes = node.children,
                expandedKeys = expandedKeys,
                inlineExpandedKeys = inlineExpandedKeys,
                onToggle = onToggle,
                onInlineToggle = onInlineToggle,
                onLeafClick = onLeafClick,
                onEntrySelected = onEntrySelected,
                config = config,
                showDescriptions = showDescriptions,
                depth = depth + 1,
                resolveMode = resolveMode,
            )
        }

        if (node.isLeaf && mode == GroupExpansionMode.INLINE && node.key in inlineExpandedKeys) {
            entryGridItem(node.key, node.entries, config, depth + 1, onEntrySelected)
            item(key = "${node.key}:see_all") {
                SeeAllItem(depth = depth + 1, onClick = { onLeafClick(node.key) })
            }
        }
    }
}

private fun LazyListScope.searchItems(
    nodes: List<GroupNode>,
    inlineExpandedKeys: Set<String>,
    onInlineToggle: (String) -> Unit,
    onLeafClick: (String) -> Unit,
    onEntrySelected: (String) -> Unit,
    config: PreviewConfig,
    showDescriptions: Boolean,
    resolveMode: (GroupNode) -> GroupExpansionMode,
) {
    for (node in nodes) {
        val mode = resolveMode(node)
        val isExpanded = node.key in inlineExpandedKeys

        item(key = node.key) {
            GroupListItem(
                node = node,
                isExpanded = isExpanded,
                isLeaf = true,
                expansionMode = mode,
                depth = 0,
                showDescriptions = showDescriptions,
                onClick = {
                    when (mode) {
                        GroupExpansionMode.INLINE -> onInlineToggle(node.key)
                        GroupExpansionMode.SUBSCREEN -> onLeafClick(node.key)
                    }
                },
            )
        }

        if (mode == GroupExpansionMode.INLINE && node.key in inlineExpandedKeys) {
            entryGridItem(node.key, node.entries, config, 1, onEntrySelected)
            item(key = "${node.key}:see_all") {
                SeeAllItem(depth = 1, onClick = { onLeafClick(node.key) })
            }
        }
    }
}

private fun LazyListScope.entryGridItem(
    nodeKey: String,
    entries: List<PreviewEntry>,
    config: PreviewConfig,
    depth: Int,
    onEntrySelected: (String) -> Unit,
) {
    item(key = "$nodeKey:grid") {
        val settings = LocalResolvedSettings.current
        val groupClass = entries.firstOrNull()?.group?.let { it::class }
        val groupConfig = groupClass?.let { config.groupOverrides[it] }
        val columns = (groupConfig?.thumbnailColumns ?: settings.thumbnailColumns).coerceAtLeast(1)
        EntryGrid(
            entries = entries,
            columns = columns,
            previewWrapperFor = { entry -> resolvePreviewWrapper(entry, config) },
            showTags = settings.showTags,
            depth = depth,
            onEntrySelected = onEntrySelected,
        )
    }
}

@Composable
private fun EntryGrid(
    entries: List<PreviewEntry>,
    columns: Int,
    previewWrapperFor: (PreviewEntry) -> PreviewWrapper?,
    showTags: Boolean,
    depth: Int,
    onEntrySelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(start = (depth * 16).dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.chunked(columns).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowEntries.forEach { entry ->
                    PreviewThumbnailCard(
                        entry = entry,
                        previewWrapper = previewWrapperFor(entry),
                        showTags = showTags,
                        onClick = { onEntrySelected(entry.familyKey()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill trailing empty cells in an incomplete last row
                repeat(columns - rowEntries.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun resolvePreviewWrapper(
    entry: PreviewEntry,
    config: PreviewConfig,
): PreviewWrapper? {
    val previewOverride = config.previewOverrides[entry.key]
    val groupConfig = config.groupOverrides[entry.group::class]
    return previewOverride?.previewWrapper ?: groupConfig?.previewWrapper ?: config.previewWrapper
}

// ── Item composables ──────────────────────────────────────────────────────────

@Composable
private fun GroupListItem(
    node: GroupNode,
    isExpanded: Boolean,
    isLeaf: Boolean,
    expansionMode: GroupExpansionMode,
    depth: Int,
    showDescriptions: Boolean,
    onClick: () -> Unit,
) {
    val count = node.totalCount
    val showChevron = !isLeaf || expansionMode == GroupExpansionMode.INLINE

    ListItem(
        modifier = Modifier
            .padding(start = (depth * 16).dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        headlineContent = { Text(node.name) },
        supportingContent = if (showDescriptions && node.description.isNotEmpty()) {
            { Text(node.description, style = MaterialTheme.typography.bodySmall) }
        } else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                if (showChevron) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown
                        else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
    )
    HorizontalDivider(modifier = Modifier.padding(start = (depth * 16 + 16).dp))
}

@Composable
private fun SeeAllItem(depth: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "See all in group",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "See all previews in this group",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    HorizontalDivider(modifier = Modifier.padding(start = (depth * 16 + 16).dp))
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun resolveExpansionMode(node: GroupNode, config: PreviewConfig): GroupExpansionMode {
    if (!node.isLeaf) return GroupExpansionMode.SUBSCREEN
    val groupClass = node.entries.firstOrNull()?.group?.let { it::class }
        ?: return config.groupExpansionMode
    return config.groupOverrides[groupClass]?.expansionMode ?: config.groupExpansionMode
}

/**
 * Returns leaf [GroupNode]s that satisfy [query] and [selectedTags].
 *
 * An entry passes if:
 * - it has at least one of [selectedTags] (when any are selected), **and**
 * - it matches [query] by name, description, or tag substring (when query is non-blank).
 *
 * Ancestor group-name/description text matching is only applied when [selectedTags] is empty,
 * so tag filtering always resolves at entry level.
 */
private fun filterNodes(
    nodes: List<GroupNode>,
    query: String,
    selectedTags: Set<String>,
    favoriteFamilyKeys: Set<String>,
    favoritesOnly: Boolean,
    flavoredOnly: Boolean,
): List<GroupNode> {
    val q = query.trim().lowercase()
    if (q.isEmpty() && selectedTags.isEmpty()) return nodes

    fun PreviewEntry.passes(): Boolean {
        if (favoritesOnly && familyKey() !in favoriteFamilyKeys) return false
        if (flavoredOnly && variantGroup == null) return false
        if (selectedTags.isNotEmpty() && tags.none { it.tagKey() in selectedTags }) return false
        if (q.isNotEmpty()) {
            return name.lowercase().contains(q) ||
                    description.lowercase().contains(q) ||
                    tags.any { it.title.lowercase().contains(q) }
        }
        return true
    }

    fun collect(nodes: List<GroupNode>, ancestorMatchesText: Boolean): List<GroupNode> {
        val result = mutableListOf<GroupNode>()
        for (node in nodes) {
            val selfMatchesText = q.isNotEmpty() && (
                    node.name.lowercase().contains(q) ||
                            node.description.lowercase().contains(q)
                    )
            // Ancestor/self text matching is only honoured when no tag filter is active,
            // otherwise we always require at least one entry to pass both filters.
            val includeByAncestor =
                selectedTags.isEmpty() && (selfMatchesText || ancestorMatchesText)
            if (node.isLeaf) {
                // When the group matched by name/ancestor, show all its entries; otherwise
                // show only the entries that individually satisfy the query and tag filters.
                val matchingEntries =
                    if (includeByAncestor) node.entries else node.entries.filter { it.passes() }
                if (matchingEntries.isNotEmpty()) result.add(node.copy(entries = matchingEntries))
            } else {
                result.addAll(collect(node.children, selfMatchesText || ancestorMatchesText))
            }
        }
        return result
    }

    return collect(nodes, ancestorMatchesText = false)
}
