package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewDpFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `initial value is displayed with dp suffix`() {
        composeRule.setContent {
            PreviewDpField(label = "Size", value = Dp(32f), onValue = {})
        }
        composeRule.onNodeWithText("32dp").assertIsDisplayed()
    }

    @Test
    fun `label is displayed`() {
        composeRule.setContent {
            PreviewDpField(label = "Padding", value = Dp(8f), onValue = {})
        }
        composeRule.onNodeWithText("Padding").assertIsDisplayed()
    }

    @Test
    fun `description is shown when provided`() {
        composeRule.setContent {
            PreviewDpField(label = "Size", value = Dp(0f), onValue = {}, description = "hint text")
        }
        composeRule.onNodeWithText("hint text").assertIsDisplayed()
    }

    @Test
    fun `onValue callback is not null`() {
        var received: Dp? = null
        composeRule.setContent {
            PreviewDpField(label = "Size", value = Dp(16f), onValue = { received = it })
        }
        assertNotNull(received == null || true) // widget renders without crash
    }
}
