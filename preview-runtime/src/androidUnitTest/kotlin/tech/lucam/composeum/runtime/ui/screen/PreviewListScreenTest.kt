package tech.lucam.composeum.runtime.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.annotation.PreviewTag
import tech.lucam.composeum.annotation.SimplePreviewTag
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.GroupConfig
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.store.ResolvedSettings
import tech.lucam.composeum.runtime.ui.component.LocalResolvedSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class PreviewListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // --- Test fixtures ---

    sealed interface TestGroup : PreviewGroup {
        data object Components : TestGroup {
            override val name = "Components"
        }

        data object Screens : TestGroup {
            override val name = "Screens"
        }
    }

    private fun tags(vararg values: String): List<PreviewTag> = values.map(::SimplePreviewTag)

    private fun entry(
        name: String,
        group: PreviewGroup = TestGroup.Components,
        tags: List<PreviewTag> = emptyList(),
    ) = PreviewEntry(
        key = "test.${group::class.simpleName}.$name",
        name = name,
        group = group,
        description = "",
        tags = tags,
        composable = { Text(name) },
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    private fun registryOf(vararg entries: PreviewEntry): PreviewRegistry =
        object : PreviewRegistry { override val entries = entries.toList() }

    private val componentsKey = TestGroup.Components::class.qualifiedName!!
    private val screensKey = TestGroup.Screens::class.qualifiedName!!
    private val defaultConfig = PreviewConfig()
    private val defaultSettings = ResolvedSettings.DEFAULT

    // --- Cards rendered for each entry in group ---

    @Test
    fun `all entries for the group are rendered as cards`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewListScreen(
                    groupKey = componentsKey,
                    registry = registryOf(
                        entry("PrimaryButton"),
                        entry("SecondaryButton"),
                        entry("UnrelatedScreen", group = TestGroup.Screens),
                    ),
                    config = defaultConfig,
                    onEntrySelected = {},
                )
            }
        }

        composeRule.onNodeWithText("PrimaryButton").assertIsDisplayed()
        composeRule.onNodeWithText("SecondaryButton").assertIsDisplayed()
        composeRule.onNodeWithText("UnrelatedScreen").assertDoesNotExist()
    }

    @Test
    fun `empty group shows no-previews message`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewListScreen(
                    groupKey = screensKey,
                    registry = registryOf(entry("Button", group = TestGroup.Components)),
                    config = defaultConfig,
                    onEntrySelected = {},
                )
            }
        }

        composeRule.onNodeWithText("No previews in this group.").assertIsDisplayed()
    }

    // --- Column count from settings ---

    @Test
    fun `cards are rendered regardless of thumbnail column count`() {
        val twoColumnSettings = defaultSettings.copy(thumbnailColumns = 2)

        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalResolvedSettings provides twoColumnSettings) {
                    PreviewListScreen(
                        groupKey = componentsKey,
                        registry = registryOf(
                            entry("CardA"),
                            entry("CardB"),
                            entry("CardC"),
                        ),
                        config = defaultConfig,
                        onEntrySelected = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("CardA").assertIsDisplayed()
        composeRule.onNodeWithText("CardB").assertIsDisplayed()
        composeRule.onNodeWithText("CardC").assertIsDisplayed()
    }

    @Test
    fun `group config override takes precedence over settings for column count`() {
        val settingsWithOneColumn = defaultSettings.copy(thumbnailColumns = 1)
        val configWithThreeColumns = PreviewConfig(
            groupOverrides = mapOf(
                TestGroup.Components::class to GroupConfig(thumbnailColumns = 3),
            ),
        )

        // Both entries must appear regardless of column count — verifies
        // that config override doesn't break rendering.
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalResolvedSettings provides settingsWithOneColumn) {
                    PreviewListScreen(
                        groupKey = componentsKey,
                        registry = registryOf(entry("A"), entry("B")),
                        config = configWithThreeColumns,
                        onEntrySelected = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("A").assertIsDisplayed()
        composeRule.onNodeWithText("B").assertIsDisplayed()
    }

    // --- Tap navigates ---

    @Test
    fun `tapping a card calls onEntrySelected with the entry key`() {
        var selectedKey: String? = null
        val e = entry("PrimaryButton")

        composeRule.setContent {
            MaterialTheme {
                PreviewListScreen(
                    groupKey = componentsKey,
                    registry = registryOf(e),
                    config = defaultConfig,
                    onEntrySelected = { selectedKey = it },
                )
            }
        }

        composeRule.onNodeWithText("PrimaryButton").performClick()

        assertEquals(e.key, selectedKey)
    }

    @Test
    fun `tapping different cards calls onEntrySelected with each entry key`() {
        val keys = mutableListOf<String>()
        val e1 = entry("Alpha")
        val e2 = entry("Beta")

        composeRule.setContent {
            MaterialTheme {
                PreviewListScreen(
                    groupKey = componentsKey,
                    registry = registryOf(e1, e2),
                    config = defaultConfig,
                    onEntrySelected = { keys.add(it) },
                )
            }
        }

        composeRule.onNodeWithText("Alpha").performClick()
        composeRule.onNodeWithText("Beta").performClick()

        assertEquals(listOf(e1.key, e2.key), keys)
    }

    // --- Tags ---

    @Test
    fun `tag chips are shown when showTags is true`() {
        val settingsWithTags = defaultSettings.copy(showTags = true)

        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalResolvedSettings provides settingsWithTags) {
                    PreviewListScreen(
                        groupKey = componentsKey,
                        registry = registryOf(entry("Button", tags = tags("cta", "primary"))),
                        config = defaultConfig,
                        onEntrySelected = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("cta").assertIsDisplayed()
        composeRule.onNodeWithText("primary").assertIsDisplayed()
    }

    @Test
    fun `tag chips are hidden when showTags is false`() {
        val settingsNoTags = defaultSettings.copy(showTags = false)

        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalResolvedSettings provides settingsNoTags) {
                    PreviewListScreen(
                        groupKey = componentsKey,
                        registry = registryOf(entry("Button", tags = tags("cta"))),
                        config = defaultConfig,
                        onEntrySelected = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("cta").assertDoesNotExist()
    }

    // --- previewWrapper ---

    @Test
    fun `global previewWrapper is applied around each card composable`() {
        val config = PreviewConfig(
            previewWrapper = { _, content ->
                Text("wrapper-sentinel")
                content()
            },
        )

        composeRule.setContent {
            MaterialTheme {
                PreviewListScreen(
                    groupKey = componentsKey,
                    registry = registryOf(entry("Button")),
                    config = config,
                    onEntrySelected = {},
                )
            }
        }

        composeRule.onNodeWithText("wrapper-sentinel").assertIsDisplayed()
    }
}

// Extension to make ResolvedSettings copyable in tests
private fun ResolvedSettings.copy(
    thumbnailColumns: Int = this.thumbnailColumns,
    showTags: Boolean = this.showTags,
) = ResolvedSettings(
    isDark = this.isDark,
    theme = this.theme,
    fontScale = this.fontScale,
    uiScale = this.uiScale,
    thumbnailColumns = thumbnailColumns,
    showDescriptions = this.showDescriptions,
    showTags = showTags,
    locale = this.locale,
)
