package tech.lucam.composeum.runtime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.AccessibilityPreviewState
import tech.lucam.composeum.runtime.ColorBlindMode
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.GroupExpansionMode
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.ui.component.LocalAccessibilityPreviewState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
        object : PreviewRegistry { override val entries = entries.toList() }

    // --- Initial state ---

    @Test
    fun `GroupListScreen is shown initially`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        // Search field is the hallmark of GroupListScreen
        composeRule.onNode(hasSetTextAction(), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `top app bar shows app title on root screen`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        composeRule.onNodeWithText("Compose Preview").assertIsDisplayed()
    }

    @Test
    fun `back button is not shown on root screen`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Navigate back").assertDoesNotExist()
    }

    // --- Back navigation ---

    @Test
    fun `tapping a group navigates to PreviewListScreen`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick()

        // PreviewListScreen shows the entry thumbnail card with the entry name
        composeRule.onNodeWithText("ButtonA").assertIsDisplayed()
    }

    @Test
    fun `back button navigates from PreviewListScreen to GroupListScreen`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        // Navigate to PreviewListScreen
        composeRule.onNodeWithText("Components").performClick()

        // Press back
        composeRule.onNodeWithContentDescription("Navigate back").performClick()

        // Should be back on GroupListScreen — search bar is visible again
        composeRule.onNode(hasSetTextAction(), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `back button is shown on non-root screens`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick()

        composeRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
    }

    // --- Settings sheet ---

    @Test
    fun `settings icon is shown on root screen`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open settings").assertIsDisplayed()
    }

    @Test
    fun `settings sheet opens when settings icon is tapped`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open settings").performClick()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun `settings icon is shown on non-root screens`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

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
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entryWithSource("ButtonA")),
                    config = PreviewConfig(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open source file").assertDoesNotExist()
    }

    @Test
    fun `source link icon is not shown on list screen`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entryWithSource("ButtonA")),
                    config = PreviewConfig(groupExpansionMode = GroupExpansionMode.SUBSCREEN),
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick()

        composeRule.onNodeWithContentDescription("Open source file").assertDoesNotExist()
    }

    @Test
    fun `source link icon is shown on detail screen when sourceFile is set`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entryWithSource("ButtonA")),
                    config = PreviewConfig(groupExpansionMode = GroupExpansionMode.SUBSCREEN),
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick()
        composeRule.onNodeWithText("ButtonA").performClick()

        composeRule.onNodeWithContentDescription("Open source file").assertIsDisplayed()
    }

    @Test
    fun `source link icon is not shown on detail screen when sourceFile is empty`() {
        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(
                        entryWithSource("ButtonA", sourceFile = ""),
                    ),
                    config = PreviewConfig(groupExpansionMode = GroupExpansionMode.SUBSCREEN),
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick()
        composeRule.onNodeWithText("ButtonA").performClick()

        composeRule.onNodeWithContentDescription("Open source file").assertDoesNotExist()
    }

    @Test
    fun `previews receive resolved accessibility state from config`() {
        val entry = PreviewEntry(
            key = "test.accessibility",
            name = "Accessibility",
            group = TestGroup.Components,
            description = "",
            tags = emptyList(),
            composable = {
                val state = LocalAccessibilityPreviewState.current
                Text("sr=${state.screenReaderMode},cb=${state.colorBlindMode.name},motion=${state.reducedMotionMode}")
            },
            paramForm = null,
            paramDefaults = PreviewParamDefaults(emptyMap()),
        )

        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry),
                    config = PreviewConfig(
                        accessibilityState = AccessibilityPreviewState(
                            screenReaderMode = true,
                            colorBlindMode = ColorBlindMode.TRITANOPIA,
                            reducedMotionMode = true,
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick()
        composeRule.onNodeWithText("Accessibility").performClick()
        composeRule.onNodeWithText("sr=true,cb=TRITANOPIA,motion=true").assertIsDisplayed()
    }

    @Test
    fun `accessibility wrapper is applied to preview renders`() {
        val entry = PreviewEntry(
            key = "test.wrapper",
            name = "Wrapper",
            group = TestGroup.Components,
            description = "",
            tags = emptyList(),
            composable = { Text("Inner Preview") },
            paramForm = null,
            paramDefaults = PreviewParamDefaults(emptyMap()),
        )

        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry),
                    config = PreviewConfig(
                        accessibilityState = AccessibilityPreviewState(highContrastMode = true),
                        accessibilityWrapper = { state, _, content ->
                            WrapperProbe(state.highContrastMode, content)
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick()
        composeRule.onNodeWithText("Wrapper").performClick()
        composeRule.onNodeWithText("Wrapper highContrast=true").assertIsDisplayed()
        composeRule.onNodeWithText("Inner Preview").assertIsDisplayed()
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

        composeRule.setContent {
            MaterialTheme {
                ComposeumBrowser(
                    registry = registryOf(entry("ButtonA")),
                    config = config,
                )
            }
        }

        composeRule.waitForIdle()
        assert(wrapperInvoked) { "browserWrapper was not invoked" }
    }
}

@Composable
private fun WrapperProbe(
    highContrastEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    Text("Wrapper highContrast=$highContrastEnabled")
    content()
}
