package tech.lucam.composeum.runtime.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewRendererTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun entry(composable: @androidx.compose.runtime.Composable () -> Unit) = PreviewEntry(
        key = "test.key",
        name = "Test",
        group = object : PreviewGroup { override val name = "Test" },
        description = "",
        tags = emptyList(),
        composable = composable,
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    @Test
    fun `normal composable renders without error card`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewRenderer(entry = entry { Text("Hello preview") })
            }
        }
        composeRule.onNodeWithText("Hello preview").assertIsDisplayed()
    }

    @Test
    @Ignore("Crash-isolated preview rendering is not currently supported in Robolectric Compose tests.")
    fun `throwing composable renders error card instead of crashing`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewRenderer(entry = entry { throw RuntimeException("boom") })
            }
        }
        composeRule.onNodeWithText("RuntimeException").assertIsDisplayed()
    }

    @Test
    @Ignore("Crash-isolated preview rendering is not currently supported in Robolectric Compose tests.")
    fun `error card shows exception message`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewRenderer(entry = entry { throw IllegalStateException("bad state") })
            }
        }
        composeRule.onNodeWithText("bad state").assertIsDisplayed()
    }
}
