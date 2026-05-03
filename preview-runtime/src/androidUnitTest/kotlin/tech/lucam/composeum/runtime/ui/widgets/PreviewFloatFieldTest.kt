package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewFloatFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `label and formatted value are displayed`() {
        composeRule.setContent {
            PreviewFloatField(label = "Opacity", value = 0.5f, onValue = {}, range = 0f..1f)
        }
        composeRule.onNodeWithText("Opacity").assertExists()
        composeRule.onNodeWithText("0.50").assertExists()
        composeRule.onNodeWithContentDescription("Opacity").assertExists()
    }

    @Test
    fun `onValue fires when slider progress changes`() {
        var received: Float? = null
        composeRule.setContent {
            PreviewFloatField(
                label = "Opacity",
                value = 0f,
                onValue = { received = it },
                range = 0f..1f
            )
        }
        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(0f, 0f..1f, 0),
                )
            )
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }
        assertNotNull(received)
        assertTrue("Expected value near 0.75, got $received", received!! in 0.7f..0.8f)
    }
}
