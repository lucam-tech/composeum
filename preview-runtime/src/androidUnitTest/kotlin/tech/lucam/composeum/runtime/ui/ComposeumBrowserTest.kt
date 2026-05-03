package tech.lucam.composeum.runtime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.GroupExpansionMode
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.familyKey
import tech.lucam.composeum.runtime.groupKey
import tech.lucam.composeum.runtime.store.RuntimeSettings
import tech.lucam.composeum.runtime.store.SettingsStorage

@RunWith(RobolectricTestRunner::class)
class ComposeumBrowserTest {

    @get:Rule
    val composeRule = createComposeRule()

    // --- Fixtures ---

    sealed interface TestGroup : PreviewGroup {
        data object Components : TestGroup {
            override val name = "Components"
        }
    }

    private fun entry(name: String, group: PreviewGroup = TestGroup.Components) = PreviewEntry(
        key = "test.$name",
        name = name,
        group = group,
        description = "",
        tags = emptyList(),
        composable = { Text(name) },
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    private fun registryOf(vararg entries: PreviewEntry): PreviewRegistry =
        object : PreviewRegistry {
            override val entries = entries.toList()
        }

    private fun setBrowserContent(
        vararg entries: PreviewEntry,
        config: PreviewConfig = PreviewConfig(),
        storage: SettingsStorage = FakeSettingsStorage(),
    ) {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(*entries),
                    config = config,
                    storage = storage,
                )
            }
        }
    }

    private class FakeSettingsStorage(
        initial: RuntimeSettings = RuntimeSettings(),
    ) : SettingsStorage {
        private val state = MutableStateFlow(initial)

        override val settings: Flow<RuntimeSettings> = state

        override suspend fun update(block: RuntimeSettings.() -> RuntimeSettings) {
            state.value = state.value.block()
        }

        override suspend fun reset() {
            state.value = RuntimeSettings()
        }
    }

    // --- Initial state ---

    @Test
    fun `GroupListScreen is shown initially`() {
        setBrowserContent(entry("ButtonA"))

        // Search field is the hallmark of GroupListScreen
        composeRule.onNode(hasSetTextAction(), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `top app bar shows app title on root screen`() {
        setBrowserContent(entry("ButtonA"))

        composeRule.onNodeWithText("Compose Preview").assertIsDisplayed()
    }

    @Test
    fun `back button is not shown on root screen`() {
        setBrowserContent(entry("ButtonA"))

        composeRule.onNodeWithContentDescription("Navigate back").assertDoesNotExist()
    }

    // --- Back navigation ---

    @Test
    fun `tapping a group navigates to PreviewListScreen`() {
        setBrowserContent(entry("ButtonA"))

        composeRule.onNodeWithText("Components").performClick()

        // PreviewListScreen shows the entry thumbnail card with the entry name
        composeRule.onNodeWithText("ButtonA").assertIsDisplayed()
    }

    @Test
    fun `back button navigates from PreviewListScreen to GroupListScreen`() {
        setBrowserContent(entry("ButtonA"))

        // Navigate to PreviewListScreen
        composeRule.onNodeWithText("Components").performClick()

        // Press back
        composeRule.onNodeWithContentDescription("Navigate back").performClick()

        // Should be back on GroupListScreen — search bar is visible again
        composeRule.onNode(hasSetTextAction(), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `back button is shown on non-root screens`() {
        setBrowserContent(entry("ButtonA"))

        composeRule.onNodeWithText("Components").performClick()

        composeRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
    }

    // --- Settings sheet ---

    @Test
    fun `settings icon is shown on root screen`() {
        setBrowserContent(entry("ButtonA"))

        composeRule.onNodeWithContentDescription("Open settings").assertIsDisplayed()
    }

    @Test
    fun `settings sheet opens when settings icon is tapped`() {
        setBrowserContent(entry("ButtonA"))

        composeRule.onNodeWithContentDescription("Open settings").performClick()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun `settings icon is shown on non-root screens`() {
        setBrowserContent(entry("ButtonA"))

        composeRule.onNodeWithText("Components").performClick()

        composeRule.onNodeWithContentDescription("Open settings").assertIsDisplayed()
    }

    // --- Source link icon ---

    private fun entryWithSource(
        name: String,
        sourceFile: String = "/project/src/main/kotlin/Foo.kt",
        sourceLine: Int = 42,
    ) = PreviewEntry(
        key = "test.$name",
        name = name,
        group = TestGroup.Components,
        description = "",
        tags = emptyList(),
        composable = { Text(name) },
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
        sourceFile = sourceFile,
        sourceLine = sourceLine,
    )

    @Test
    fun `source link icon is not shown on root screen`() {
        setBrowserContent(entryWithSource("ButtonA"))

        composeRule.onNodeWithContentDescription("Open source file").assertDoesNotExist()
    }

    @Test
    fun `source link icon is not shown on list screen`() {
        setBrowserContent(
            entryWithSource("ButtonA"),
            config = PreviewConfig(groupExpansionMode = GroupExpansionMode.SUBSCREEN),
        )

        composeRule.onNodeWithText("Components").performClick()

        composeRule.onNodeWithContentDescription("Open source file").assertDoesNotExist()
    }

    @Test
    fun `source link icon is shown on detail screen when sourceFile is set`() {
        setBrowserContent(
            entryWithSource("ButtonA"),
            config = PreviewConfig(groupExpansionMode = GroupExpansionMode.SUBSCREEN),
        )

        composeRule.onNodeWithText("Components").performClick()
        composeRule.onNodeWithText("ButtonA").performClick()

        composeRule.onNodeWithContentDescription("Open source file").assertIsDisplayed()
    }

    @Test
    fun `source link icon is not shown on detail screen when sourceFile is empty`() {
        setBrowserContent(
            entryWithSource("ButtonA", sourceFile = ""),
            config = PreviewConfig(groupExpansionMode = GroupExpansionMode.SUBSCREEN),
        )

        composeRule.onNodeWithText("Components").performClick()
        composeRule.onNodeWithText("ButtonA").performClick()

        composeRule.onNodeWithContentDescription("Open source file").assertDoesNotExist()
    }

    @Test
    fun `persisted route stores screen path without share state`() {
        val storage = FakeSettingsStorage()
        val detailEntry = entry("ButtonA")

        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(detailEntry),
                    config = PreviewConfig(),
                    storage = storage,
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick()
        composeRule.onNodeWithText("ButtonA").performClick()

        val expectedRoute = "preview_detail/${detailEntry.familyKey()}"
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { storage.settings.first().lastRoute == expectedRoute }
        }
        val settings = runBlocking { storage.settings.first() }
        assertEquals(
            expectedRoute,
            settings.lastRoute,
        )
    }

    // --- Browser wrapper ---

    @Test
    fun `browserWrapper is applied around nav content`() {
        var wrapperInvoked = false

        val config = PreviewConfig(
            browserWrapper = { content ->
                wrapperInvoked = true
                content()
            },
        )

        setBrowserContent(entry("ButtonA"), config = config)

        composeRule.runOnIdle {
            assertTrue("browserWrapper was not invoked", wrapperInvoked)
        }
    }
}
