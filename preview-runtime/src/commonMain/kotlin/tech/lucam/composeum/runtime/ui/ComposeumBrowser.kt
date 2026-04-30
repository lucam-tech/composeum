package tech.lucam.composeum.runtime.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.PreviewParamState
import tech.lucam.composeum.runtime.familyKey
import tech.lucam.composeum.runtime.families
import tech.lucam.composeum.runtime.toPreviewParamState
import tech.lucam.composeum.runtime.toShareableMap
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.ThemeOption
import tech.lucam.composeum.runtime.config.ThemeOptionDefaults
import tech.lucam.composeum.runtime.config.mergedWith
import tech.lucam.composeum.runtime.ui.component.LocalAccessibilityPreviewState
import tech.lucam.composeum.runtime.store.RuntimeSettings
import tech.lucam.composeum.runtime.store.SettingsStorage
import tech.lucam.composeum.runtime.store.SettingsViewModel
import tech.lucam.composeum.runtime.ui.component.LocalPreviewConfig
import tech.lucam.composeum.runtime.ui.component.LocalResolvedSettings
import tech.lucam.composeum.runtime.ui.component.LocalRuntimeSettings
import tech.lucam.composeum.runtime.ui.component.LocalSettingsStorage
import tech.lucam.composeum.runtime.ui.component.SettingsSheet
import tech.lucam.composeum.runtime.ui.component.SourceLocationDialog
import tech.lucam.composeum.runtime.ui.screen.GroupListScreen
import tech.lucam.composeum.runtime.ui.screen.PreviewDetailScreen
import tech.lucam.composeum.runtime.ui.screen.PreviewListScreen
import kotlinx.coroutines.launch

/**
 * Composition local exposing the resolved dark-mode flag to [BrowserWrapper] lambdas.
 *
 * Provided by [ComposeumBrowser] before invoking [PreviewConfig.browserWrapper], so custom
 * wrappers can read it and pass it to their own `MaterialTheme`:
 *
 * ```kotlin
 * browserWrapper { content ->
 *     val isDark = LocalIsDarkTheme.current
 *     AppTheme(darkTheme = isDark) { content() }
 * }
 * ```
 */
val LocalIsDarkTheme = compositionLocalOf { false }

/** Composition local exposing the resolved theme palette to [BrowserWrapper] lambdas. */
val LocalPreviewTheme = compositionLocalOf<ThemeOption> { ThemeOptionDefaults.Classic }

/**
 * The root composable for the preview browser.
 * Place this in your catalog Activity's `setContent` block (Android) or `renderComposable`
 * entry point (wasmJs).
 *
 * @param registry The registry of all previews to browse.
 * @param config   Configuration for the browser. Build with [previewConfig].
 * @param storage  Platform-specific settings persistence. Use [DataStoreSettingsStorage] on
 *                 Android (or the convenience Android-only overload) and
 *                 [LocalStorageSettingsStorage] on wasmJs.
 * @param modifier Modifier applied to the root scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeumBrowser(
    registry: PreviewRegistry,
    config: PreviewConfig = PreviewConfig(),
    storage: SettingsStorage,
    modifier: Modifier = Modifier,
) {
    val effectiveConfig = remember(registry, config) { registry.config.mergedWith(config) }
    val scope = rememberCoroutineScope()

    // Bridge isSystemInDarkTheme() — a composable — into a flow so SettingsViewModel
    // can combine it with the persisted ThemeOverride when resolving isDark.
    val systemIsDark = isSystemInDarkTheme()
    val systemIsDarkFlow = remember { snapshotFlow { systemIsDark } }

    val viewModel = remember(storage, effectiveConfig) {
        SettingsViewModel(storage, effectiveConfig, scope, systemIsDarkFlow)
    }

    val resolvedSettings by viewModel.resolvedSettings.collectAsState()
    val runtimeSettings by storage.settings.collectAsState(initial = RuntimeSettings())

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    var activeDetailEntryKey by remember { mutableStateOf<String?>(null) }
    var activeDetailParamState by remember { mutableStateOf<PreviewParamState?>(null) }
    val currentShareableState = decodeShareablePreviewState(
        navArgumentValue(currentBackStack, PreviewRoute.STATE_ARG),
    )

    val isRoot = currentRoute == null || currentRoute == PreviewRoute.GroupList.route

    // Non-null only when the detail screen is active; used for title and the source link button.
    val currentDetailEntry = when (currentRoute) {
        PreviewRoute.PreviewDetail("").route -> {
            val familyKey = navArgumentValue(currentBackStack, PreviewRoute.PreviewDetail.ARG)
            val selectedEntryKey = activeDetailEntryKey ?: currentShareableState?.selectedEntryKey
            val activeEntry = selectedEntryKey?.let { entryKey ->
                registry.entries.firstOrNull { it.key == entryKey && it.familyKey() == familyKey }
            }
            activeEntry ?: registry.families().find { it.key == familyKey }?.defaultEntry
        }
        else -> null
    }

    val title = when (currentRoute) {
        PreviewRoute.GroupList.route -> "Compose Preview"
        PreviewRoute.PreviewList("").route -> {
            val groupKey = navArgumentValue(currentBackStack, PreviewRoute.PreviewList.ARG)
            registry.entries
                .firstOrNull { (it.group::class.qualifiedName ?: it.group.name) == groupKey }
                ?.group?.name ?: groupKey
        }
        PreviewRoute.PreviewDetail("").route -> currentDetailEntry?.name ?: ""
        else -> "Compose Preview"
    }

    var showSettings by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val favoriteFamilyKeys = runtimeSettings.favoriteFamilyKeys.toSet()
    val currentFamilyKey = currentDetailEntry?.familyKey()
    val isFavorite = currentFamilyKey != null && currentFamilyKey in favoriteFamilyKeys
    val initialRoute = effectiveConfig.initialRoute ?: runtimeSettings.lastRoute
    var restoredInitialRoute by remember(initialRoute) { mutableStateOf(false) }

    LaunchedEffect(navController, initialRoute, restoredInitialRoute) {
        if (!restoredInitialRoute && !initialRoute.isNullOrBlank()) {
            restoredInitialRoute = true
            navController.navigate(initialRoute) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(currentShareableState) {
        val state = currentShareableState ?: return@LaunchedEffect
        storage.update {
            copy(
                themeOverride = state.themeOverride ?: themeOverride,
                themeId = state.themeId ?: themeId,
                fontScale = state.fontScale ?: fontScale,
                uiScale = state.uiScale ?: uiScale,
                locale = state.locale ?: locale,
                screenReaderMode = state.accessibilityState.screenReaderMode,
                highContrastMode = state.accessibilityState.highContrastMode,
                colorBlindMode = state.accessibilityState.colorBlindMode,
                reducedMotionMode = state.accessibilityState.reducedMotionMode,
                largeTouchTargetsMode = state.accessibilityState.largeTouchTargetsMode,
            )
        }
    }

    val browserContent: @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalResolvedSettings provides resolvedSettings,
            LocalRuntimeSettings provides runtimeSettings,
            LocalSettingsStorage provides storage,
            LocalPreviewConfig provides effectiveConfig,
            LocalAccessibilityPreviewState provides resolvedSettings.accessibilityState,
        ) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            if (!isRoot) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Navigate back",
                                    )
                                }
                            }
                        },
                        actions = {
                            effectiveConfig.topBarActions.forEach { action ->
                                IconButton(onClick = action.onClick) {
                                    action.icon()
                                }
                            }
                            val detailEntry = currentDetailEntry
                            if (detailEntry != null && detailEntry.sourceFile.isNotEmpty()) {
                                IconButton(onClick = { showSourceDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Open source file",
                                    )
                                }
                            }
                            if (detailEntry != null) {
                                IconButton(
                                    onClick = {
                                        val familyKey = detailEntry.familyKey()
                                        scope.launch {
                                            storage.update {
                                                val nextFavorites = if (familyKey in favoriteFamilyKeys) {
                                                    favoriteFamilyKeys - familyKey
                                                } else {
                                                    favoriteFamilyKeys + familyKey
                                                }.toList()
                                                copy(favoriteFamilyKeys = nextFavorites)
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = if (isFavorite) {
                                            "Remove preview from favorites"
                                        } else {
                                            "Add preview to favorites"
                                        },
                                    )
                                }
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open settings",
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = PreviewRoute.GroupList.route,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable(route = PreviewRoute.GroupList.route) {
                        GroupListScreen(
                            registry = registry,
                            config = effectiveConfig,
                            onGroupSelected = { groupKey ->
                                navController.navigate(PreviewRoute.PreviewList.routeFor(groupKey))
                            },
                            onEntrySelected = { familyKey ->
                                activeDetailEntryKey = null
                                navController.navigate(PreviewRoute.PreviewDetail.routeFor(familyKey))
                            },
                        )
                    }
                    composable(
                        route = PreviewRoute.PreviewList("").route,
                        arguments = listOf(
                            navArgument(PreviewRoute.PreviewList.ARG) { type = NavType.StringType },
                        ),
                    ) { backStackEntry ->
                        val groupKey = navArgumentValue(backStackEntry, PreviewRoute.PreviewList.ARG)
                        PreviewListScreen(
                            groupKey = groupKey,
                            registry = registry,
                            config = effectiveConfig,
                            onEntrySelected = { familyKey ->
                                activeDetailEntryKey = null
                                navController.navigate(PreviewRoute.PreviewDetail.routeFor(familyKey))
                            },
                        )
                    }
                    composable(
                        route = PreviewRoute.PreviewDetail("").route,
                        arguments = listOf(
                            navArgument(PreviewRoute.PreviewDetail.ARG) { type = NavType.StringType },
                        ),
                    ) { backStackEntry ->
                        val familyKey = navArgumentValue(backStackEntry, PreviewRoute.PreviewDetail.ARG)
                        val routeState = decodeShareablePreviewState(
                            navArgumentValue(backStackEntry, PreviewRoute.STATE_ARG),
                        )
                        val initialEntry = routeState?.selectedEntryKey
                        val initialParamState = initialEntry?.let { entryKey ->
                            registry.entries.firstOrNull { it.key == entryKey && it.familyKey() == familyKey }
                        }?.let { entry ->
                            routeState.paramState.toPreviewParamState(entry)
                        }
                        PreviewDetailScreen(
                            familyKey = familyKey,
                            registry = registry,
                            config = effectiveConfig,
                            initialSelectedEntryKey = initialEntry,
                            initialParamState = initialParamState,
                            onActiveEntryChanged = { entry ->
                                activeDetailEntryKey = entry?.key
                                if (entry != null) {
                                    scope.launch {
                                        storage.update {
                                            val familyKeyForRecent = entry.familyKey()
                                            copy(
                                                recentFamilyKeys = (
                                                    listOf(familyKeyForRecent) +
                                                        recentFamilyKeys.filterNot { it == familyKeyForRecent }
                                                    ).take(12),
                                            )
                                        }
                                    }
                                }
                            },
                            onParamStateChanged = { _, state -> activeDetailParamState = state },
                        )
                    }
                }
            }

            if (showSettings) {
                ModalBottomSheet(
                    onDismissRequest = { showSettings = false },
                    sheetState = sheetState,
                ) {
                    SettingsSheet(config = effectiveConfig)
                }
            }

            val sourceEntry = currentDetailEntry
            if (showSourceDialog && sourceEntry != null && sourceEntry.sourceFile.isNotEmpty()) {
                SourceLocationDialog(
                    sourceFile = sourceEntry.sourceFile,
                    sourceLine = sourceEntry.sourceLine,
                    webUrl = effectiveConfig.sourceBaseUrl?.let {
                        buildWebUrl(sourceEntry.sourceFile, sourceEntry.sourceLine, effectiveConfig)
                    },
                    onDismiss = { showSourceDialog = false },
                )
            }
        }
    }

    LaunchedEffect(
        currentRoute,
        currentBackStack,
        currentDetailEntry,
        activeDetailParamState,
        resolvedSettings,
        runtimeSettings.themeOverride,
    ) {
        val routePattern = currentRoute ?: return@LaunchedEffect
        val shareState = ShareablePreviewState(
            selectedEntryKey = currentDetailEntry?.key,
            paramState = currentDetailEntry?.let { entry ->
                activeDetailParamState?.toShareableMap(entry)
            }.orEmpty(),
            themeId = resolvedSettings.theme.id,
            themeOverride = runtimeSettings.themeOverride,
            fontScale = resolvedSettings.fontScale,
            uiScale = resolvedSettings.uiScale,
            locale = resolvedSettings.locale,
            accessibilityState = resolvedSettings.accessibilityState,
        )
        val route = when (routePattern) {
            PreviewRoute.GroupList.route -> PreviewRoute.GroupList.routeFor(shareState)
            PreviewRoute.PreviewList("").route -> {
                val groupKey = navArgumentValue(currentBackStack, PreviewRoute.PreviewList.ARG)
                PreviewRoute.PreviewList.routeFor(groupKey, shareState)
            }
            PreviewRoute.PreviewDetail("").route -> {
                val familyKey = navArgumentValue(currentBackStack, PreviewRoute.PreviewDetail.ARG)
                PreviewRoute.PreviewDetail.routeFor(familyKey, shareState)
            }
            else -> return@LaunchedEffect
        }
        effectiveConfig.onShareableRouteChanged?.invoke(route)
        if (runtimeSettings.lastRoute != route) {
            storage.update { copy(lastRoute = route) }
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides resolvedSettings.isDark,
        LocalPreviewTheme provides resolvedSettings.theme,
    ) {
        if (effectiveConfig.browserWrapper != null) {
            effectiveConfig.browserWrapper(browserContent)
        } else {
            MaterialTheme(
                colorScheme = if (resolvedSettings.isDark) {
                    darkColorScheme(
                        primary = resolvedSettings.theme.primary,
                        secondary = resolvedSettings.theme.secondary,
                    )
                } else {
                    lightColorScheme(
                        primary = resolvedSettings.theme.primary,
                        secondary = resolvedSettings.theme.secondary,
                    )
                },
            ) {
                browserContent()
            }
        }
    }
}

private fun buildWebUrl(sourceFile: String, sourceLine: Int, config: PreviewConfig): String {
    val normalized = sourceFile.replace('\\', '/')
    val prefixes = buildList {
        config.sourceStripPrefix?.let(::add)
        addAll(config.sourceStripPrefixes)
    }.map { it.replace('\\', '/').trimEnd('/') }
    val relativePath = prefixes
        .sortedByDescending { it.length }
        .firstNotNullOfOrNull { prefix ->
            normalized.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
        }
        ?: normalized
    val webPath = relativePath.trimStart('/').replace(" ", "%20")
    return "${config.sourceBaseUrl!!.trimEnd('/')}/$webPath#L$sourceLine"
}

private fun navArgumentValue(
    entry: NavBackStackEntry?,
    key: String,
): String = entry?.arguments?.read { getStringOrNull(key) } ?: ""
