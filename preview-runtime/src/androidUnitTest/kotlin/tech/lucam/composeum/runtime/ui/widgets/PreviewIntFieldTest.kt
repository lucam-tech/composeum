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
class PreviewIntFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `label and current value are displayed`() {
        composeRule.setContent {
            PreviewIntField(label = "Count", value = 42, onValue = {}, range = 0..100)
        }
        composeRule.onNodeWithText("Count").assertExists()
        composeRule.onNodeWithText("42").assertExists()
        composeRule.onNodeWithContentDescription("Count").assertExists()
    }

    @Test
    fun `onValue fires when slider progress changes`() {
        var received: Int? = null
        composeRule.setContent {
            PreviewIntField(label = "Count", value = 0, onValue = { received = it }, range = 0..100)
        }
        composeRule
            .onNode(SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0f, 0f..100f, 0),
            ))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(50f) }
        assertNotNull(received)
        assertTrue("Expected value near 50, got $received", received!! in 45..55)
    }
}
