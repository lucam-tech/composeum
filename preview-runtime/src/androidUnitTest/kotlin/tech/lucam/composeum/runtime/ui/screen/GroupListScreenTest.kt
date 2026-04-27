package tech.lucam.composeum.runtime.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.GroupExpansionMode
import tech.lucam.composeum.runtime.config.PreviewConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // --- Test group hierarchy ---

    sealed interface TestGroup : PreviewGroup {
        data object Buttons : TestGroup {
            override val name = "Buttons"
            override val description = "Button components"
        }

        data object Cards : TestGroup {
            override val name = "Cards"
            override val description = "Card components"
        }

        data object Forms : TestGroup {
            override val name = "Forms"
        }
    }

    private fun entry(
        name: String,
        group: PreviewGroup,
        description: String = "",
        tags: List<String> = emptyList(),
    ) = PreviewEntry(
        key = "test.$name",
        name = name,
        group = group,
        description = description,
        tags = tags,
        composable = { Text(name) },
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    private fun registryOf(vararg entries: PreviewEntry): PreviewRegistry =
        object : PreviewRegistry {
            override val entries = entries.toList()
        }

    /** Finds the search text field using [hasSetTextAction] in the unmerged tree. */
    private fun typeInSearchField(text: String) {
        composeRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performTextReplacement(text)
    }

    // --- Groups appear ---

    @Test
    fun `group names are shown in the list`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("PrimaryButton", TestGroup.Buttons),
                        entry("DefaultCard", TestGroup.Cards),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Buttons").assertIsDisplayed()
        composeRule.onNodeWithText("Cards").assertIsDisplayed()
    }

    @Test
    fun `entry count badge shows total entries for each group`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons),
                        entry("ButtonB", TestGroup.Buttons),
                        entry("Card1", TestGroup.Cards),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        // The merged ListItem text includes the count badge value alongside the group name.
        // Buttons node merged text contains "2" (badge) + "Buttons"
        composeRule.onNode(hasText("2", substring = true) and hasText("Buttons", substring = true))
            .assertIsDisplayed()
        // Cards node merged text contains "1" (badge) + "Cards"
        composeRule.onNode(hasText("1", substring = true) and hasText("Cards", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun `search field is visible on screen`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(entry("ButtonA", TestGroup.Buttons)),
                    onGroupSelected = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction(), useUnmergedTree = true).assertIsDisplayed()
    }

    // --- Search filters ---

    @Test
    fun `search by group name shows only matching groups`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons),
                        entry("Card1", TestGroup.Cards),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        typeInSearchField("button")

        composeRule.onNodeWithText("Buttons").assertIsDisplayed()
        composeRule.onNodeWithText("Cards").assertDoesNotExist()
    }

    @Test
    fun `search by entry name shows group containing that entry`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("PrimaryButton", TestGroup.Buttons),
                        entry("DefaultCard", TestGroup.Cards),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        typeInSearchField("primary")

        composeRule.onNodeWithText("Buttons").assertIsDisplayed()
        composeRule.onNodeWithText("Cards").assertDoesNotExist()
    }

    @Test
    fun `search by tag shows group containing entry with that tag`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("PrimaryButton", TestGroup.Buttons, tags = listOf("cta", "interactive")),
                        entry("DefaultCard", TestGroup.Cards),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        typeInSearchField("cta")

        composeRule.onNodeWithText("Buttons").assertIsDisplayed()
        composeRule.onNodeWithText("Cards").assertDoesNotExist()
    }

    @Test
    fun `search with no matches shows no group items`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(entry("ButtonA", TestGroup.Buttons)),
                    onGroupSelected = {},
                )
            }
        }

        typeInSearchField("zzznomatch")

        composeRule.onNodeWithText("Buttons").assertDoesNotExist()
    }

    @Test
    fun `search by entry description shows group containing matching entry`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("FormInput", TestGroup.Forms, description = "A text input field"),
                        entry("Card", TestGroup.Cards),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        typeInSearchField("text input")

        composeRule.onNodeWithText("Forms").assertIsDisplayed()
        composeRule.onNodeWithText("Cards").assertDoesNotExist()
    }

    // --- Tap navigates ---

    @Test
    fun `tapping a leaf group calls onGroupSelected with its key`() {
        var selectedKey: String? = null

        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(entry("ButtonA", TestGroup.Buttons)),
                    onGroupSelected = { selectedKey = it },
                )
            }
        }

        composeRule.onNodeWithText("Buttons").performClick()

        assertEquals(TestGroup.Buttons::class.qualifiedName, selectedKey)
    }

    @Test
    fun `tapping a group in search results calls onGroupSelected`() {
        var selectedKey: String? = null

        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons),
                        entry("Card1", TestGroup.Cards),
                    ),
                    onGroupSelected = { selectedKey = it },
                )
            }
        }

        typeInSearchField("card")
        composeRule.onNodeWithText("Cards").performClick()

        assertEquals(TestGroup.Cards::class.qualifiedName, selectedKey)
    }

    @Test
    fun `registry with no entries shows no group items`() {
        var selected: String? = null

        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(),
                    onGroupSelected = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("Buttons").assertDoesNotExist()
        assertNull(selected)
    }

    // --- Tag filter ---

    @Test
    fun `tag chips are shown when registry has tagged entries`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons, tags = listOf("cta", "interactive")),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("cta").assertIsDisplayed()
        composeRule.onNodeWithText("interactive").assertIsDisplayed()
    }

    @Test
    fun `tag chips are not shown when no entries have tags`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(entry("ButtonA", TestGroup.Buttons)),
                    onGroupSelected = {},
                )
            }
        }

        // No chip should be rendered
        composeRule.onNodeWithText("cta").assertDoesNotExist()
    }

    @Test
    fun `selecting a tag shows only groups containing an entry with that tag`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons, tags = listOf("cta")),
                        entry("Card1", TestGroup.Cards),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("cta").performClick()

        composeRule.onNodeWithText("Buttons").assertIsDisplayed()
        composeRule.onNodeWithText("Cards").assertDoesNotExist()
    }

    @Test
    fun `deselecting a tag restores unfiltered tree`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons, tags = listOf("cta")),
                        entry("Card1", TestGroup.Cards),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("cta").performClick() // select
        composeRule.onNodeWithText("Cards").assertDoesNotExist()

        composeRule.onNodeWithText("cta").performClick() // deselect
        composeRule.onNodeWithText("Cards").assertIsDisplayed()
    }

    @Test
    fun `tag filter and text query are combined - entry must satisfy both`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("PrimaryButton", TestGroup.Buttons, tags = listOf("cta")),
                        entry("SecondaryButton", TestGroup.Buttons, tags = listOf("interactive")),
                        entry("Card1", TestGroup.Cards, tags = listOf("cta")),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        // Select "cta" tag — Buttons (PrimaryButton) and Cards both qualify
        composeRule.onNodeWithText("cta").performClick()
        composeRule.onNodeWithText("Buttons").assertIsDisplayed()
        composeRule.onNodeWithText("Cards").assertIsDisplayed()

        // Also type "button" — now only Buttons qualifies (Card1 doesn't match text)
        typeInSearchField("button")
        composeRule.onNodeWithText("Buttons").assertIsDisplayed()
        composeRule.onNodeWithText("Cards").assertDoesNotExist()
    }

    @Test
    fun `group-name text match is suppressed when tag filter is active`() {
        // Without tag filter: typing "Buttons" would match Buttons by group name
        // even if no entry matches — that ancestor behaviour must be disabled when tags are active.
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("Alpha", TestGroup.Buttons, tags = listOf("interactive")),
                        entry("Beta", TestGroup.Cards, tags = listOf("cta")),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        // Select "cta" — only Cards should appear
        composeRule.onNodeWithText("cta").performClick()

        // Type "Buttons" — even though the group name matches, no Buttons entry has "cta"
        typeInSearchField("Buttons")
        composeRule.onNodeWithText("Buttons").assertDoesNotExist()
        composeRule.onNodeWithText("Cards").assertDoesNotExist() // Cards entries don't match "Buttons"
    }

    @Test
    fun `tags from multiple entries are deduplicated in the chip row`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons, tags = listOf("cta")),
                        entry("ButtonB", TestGroup.Buttons, tags = listOf("cta", "interactive")),
                    ),
                    onGroupSelected = {},
                )
            }
        }

        // "cta" appears in both entries but should render only one chip
        composeRule.onNodeWithText("cta").assertIsDisplayed()
        // Would throw if two nodes matched — this verifies uniqueness implicitly via assertIsDisplayed
    }

    // --- Inline grid expansion ---

    @Test
    fun `inline-expanded group shows thumbnail cards for each entry`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons),
                        entry("ButtonB", TestGroup.Buttons),
                    ),
                    config = PreviewConfig(groupExpansionMode = GroupExpansionMode.INLINE),
                    onGroupSelected = {},
                    onEntrySelected = {},
                )
            }
        }

        // Expand the group inline
        composeRule.onNodeWithText("Buttons").performClick()

        // Thumbnail card names must be visible in the expanded grid
        composeRule.onNodeWithText("ButtonA").assertIsDisplayed()
        composeRule.onNodeWithText("ButtonB").assertIsDisplayed()
    }

    @Test
    fun `tapping a thumbnail card in inline expansion calls onEntrySelected`() {
        var selectedKey: String? = null

        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("ButtonA", TestGroup.Buttons),
                    ),
                    config = PreviewConfig(groupExpansionMode = GroupExpansionMode.INLINE),
                    onGroupSelected = {},
                    onEntrySelected = { selectedKey = it },
                )
            }
        }

        composeRule.onNodeWithText("Buttons").performClick()
        composeRule.onNodeWithText("ButtonA").performClick()

        assertEquals("test.ButtonA", selectedKey)
    }

    @Test
    fun `see-all row is shown below grid when group is inline-expanded`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(entry("ButtonA", TestGroup.Buttons)),
                    config = PreviewConfig(groupExpansionMode = GroupExpansionMode.INLINE),
                    onGroupSelected = {},
                    onEntrySelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Buttons").performClick()

        composeRule.onNodeWithText("See all in group").assertIsDisplayed()
    }

    @Test
    fun `tapping see-all calls onGroupSelected`() {
        var selectedKey: String? = null

        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(entry("ButtonA", TestGroup.Buttons)),
                    config = PreviewConfig(groupExpansionMode = GroupExpansionMode.INLINE),
                    onGroupSelected = { selectedKey = it },
                    onEntrySelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Buttons").performClick()
        composeRule.onNodeWithText("See all in group").performClick()

        assertEquals(TestGroup.Buttons::class.qualifiedName, selectedKey)
    }

    @Test
    fun `inline-expanded group in search results shows thumbnail cards`() {
        composeRule.setContent {
            MaterialTheme {
                GroupListScreen(
                    registry = registryOf(
                        entry("PrimaryButton", TestGroup.Buttons),
                        entry("DefaultCard", TestGroup.Cards),
                    ),
                    config = PreviewConfig(groupExpansionMode = GroupExpansionMode.INLINE),
                    onGroupSelected = {},
                    onEntrySelected = {},
                )
            }
        }

        typeInSearchField("button")
        composeRule.onNodeWithText("Buttons").performClick()

        composeRule.onNodeWithText("PrimaryButton").assertIsDisplayed()
    }
}
