package tech.lucam.composeum.runtime.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.annotation.PreviewVariantGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.config.previewConfig
import tech.lucam.composeum.runtime.ui.component.LocalPreviewParamState

@RunWith(RobolectricTestRunner::class)
class PreviewDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // --- Fixtures ---

    private object TestGroup : PreviewGroup {
        override val name = "Test"
    }

    private object TestVariantGroup : PreviewVariantGroup

    private fun registryOf(vararg entries: PreviewEntry): PreviewRegistry =
        object : PreviewRegistry {
            override val entries = entries.toList()
        }

    private val defaultConfig = PreviewConfig()

    private fun entryWithNoParams(name: String = "MyPreview") = PreviewEntry(
        key = "test.$name",
        name = name,
        group = TestGroup,
        description = "",
        tags = emptyList(),
        composable = { Text(name) },
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    private fun entryWithParams(
        name: String = "ParamPreview",
        defaultLabel: String = "hello",
    ): PreviewEntry {
        val defaults = PreviewParamDefaults(mapOf("label" to defaultLabel))
        return PreviewEntry(
            key = "test.$name",
            name = name,
            group = TestGroup,
            description = "",
            tags = emptyList(),
            composable = {
                val state = LocalPreviewParamState.current
                Text(state["label"] ?: defaultLabel)
            },
            paramForm = { state, onUpdate ->
                Text("param-form-sentinel")
                // A simple button to change the label param
                androidx.compose.material3.Button(
                    onClick = { onUpdate(state.put("label", "changed")) },
                ) {
                    Text("change-label")
                }
            },
            paramDefaults = defaults,
        )
    }

    // --- Entry not found ---

    @Test
    fun `shows not-found message when family key is missing`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = "does.not.exist",
                    registry = registryOf(entryWithNoParams()),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithText("Preview not found.").assertIsDisplayed()
    }

    // --- Basic rendering ---

    @Test
    fun `renders the composable for the given entry`() {
        val entry = entryWithNoParams("TargetPreview")

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithText("TargetPreview").assertIsDisplayed()
    }

    // --- No param form ---

    @Test
    fun `param panel is hidden when paramForm is null`() {
        val entry = entryWithNoParams()

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithText("Parameters").assertDoesNotExist()
    }

    @Test
    fun `reset button is hidden when paramForm is null`() {
        val entry = entryWithNoParams()

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Reset parameters").assertDoesNotExist()
    }

    // --- Param form shown ---

    @Test
    fun `param panel is shown when paramForm is non-null`() {
        val entry = entryWithParams()

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithText("Parameters").assertIsDisplayed()
        composeRule.onNodeWithText("param-form-sentinel").assertIsDisplayed()
    }

    @Test
    fun `reset button is shown in param panel when paramForm is non-null`() {
        val entry = entryWithParams()

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Reset parameters").assertIsDisplayed()
    }

    @Test
    fun `preview override can replace the param form for a single preview`() {
        val entry = entryWithParams()
        val config = previewConfig {
            preview(entry.key) {
                paramForm { _, _ -> Text("override-form-sentinel") }
            }
        }

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = config,
                )
            }
        }

        composeRule.onNodeWithText("override-form-sentinel").assertIsDisplayed()
        composeRule.onNodeWithText("param-form-sentinel").assertDoesNotExist()
    }

    // --- Param change re-renders composable ---

    @Test
    fun `param change re-renders composable with updated state`() {
        val entry = entryWithParams(defaultLabel = "hello")

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithText("hello").assertIsDisplayed()
        composeRule.onNodeWithText("change-label").performClick()
        composeRule.onNodeWithText("changed").assertIsDisplayed()
    }

    // --- Reset restores defaults ---

    @Test
    fun `reset button restores param state to defaults`() {
        val entry = entryWithParams(defaultLabel = "hello")

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = defaultConfig,
                )
            }
        }

        // Change the param, verify it changed
        composeRule.onNodeWithText("change-label").performClick()
        composeRule.onNodeWithText("changed").assertIsDisplayed()

        // Reset
        composeRule.onNodeWithContentDescription("Reset parameters").performClick()
        composeRule.onNodeWithText("hello").assertIsDisplayed()
    }

    // --- Collapse / expand panel ---

    @Test
    fun `param panel collapses when toggle is clicked`() {
        val entry = entryWithParams()

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = entry.key,
                    registry = registryOf(entry),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithText("param-form-sentinel").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Collapse parameters").performClick()
        composeRule.onNodeWithText("param-form-sentinel").assertDoesNotExist()
    }

    @Test
    fun `default variant is selected first and dropdown switches variants`() {
        val base = entryWithNoParams("Base").copy(
            key = "test.base",
            variantGroup = TestVariantGroup,
            isDefaultVariant = true,
            composable = { Text("Base Content") },
        )
        val flavored = entryWithNoParams("Experimental").copy(
            key = "test.experimental",
            variantGroup = TestVariantGroup,
            composable = { Text("Experimental Content") },
        )

        composeRule.setContent {
            MaterialTheme {
                PreviewDetailScreen(
                    familyKey = TestVariantGroup::class.qualifiedName!!,
                    registry = registryOf(flavored, base),
                    config = defaultConfig,
                )
            }
        }

        composeRule.onNodeWithText("Base Content").assertIsDisplayed()
        composeRule.onNodeWithText("Base").performClick()
        composeRule.onNodeWithText("Experimental").performClick()
        composeRule.onNodeWithText("Experimental Content").assertIsDisplayed()
    }
}
