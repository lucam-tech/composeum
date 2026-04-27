package tech.lucam.composeum.runtime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.PreviewConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeumBrowserActivityTest {

    @get:Rule
    val activityRule = createAndroidComposeRule<TestBrowserActivity>()

    @Test
    fun `activity launches and renders browser without crash`() {
        activityRule.onNodeWithText("Compose Preview").assertIsDisplayed()
    }

    class TestBrowserActivity : ComposeumBrowserActivity() {

        sealed interface TestGroup : PreviewGroup {
            data object Components : TestGroup {
                override val name = "Components"
            }
        }

        override val registry: PreviewRegistry = object : PreviewRegistry {
            override val entries = listOf(
                PreviewEntry(
                    key = "test.SampleButton",
                    name = "Sample Button",
                    group = TestGroup.Components,
                    description = "",
                    tags = emptyList(),
                    composable = { Text("Sample Button") },
                    paramForm = null,
                    paramDefaults = PreviewParamDefaults(emptyMap()),
                ),
            )
        }

        override val config: PreviewConfig = PreviewConfig(
            browserWrapper = { content -> MaterialTheme { content() } },
        )
    }
}
