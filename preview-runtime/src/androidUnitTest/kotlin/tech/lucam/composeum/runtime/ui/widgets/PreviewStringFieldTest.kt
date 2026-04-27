package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewStringFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `initial value is displayed in text field`() {
        composeRule.setContent {
            PreviewStringField(label = "Name", value = "hello", onValue = {})
        }
        composeRule.onNodeWithText("hello").assertIsDisplayed()
    }

    @Test
    fun `onValue fires when text changes`() {
        var received = ""
        composeRule.setContent {
            PreviewStringField(label = "Name", value = "hello", onValue = { received = it })
        }
        composeRule.onNodeWithText("hello").performTextReplacement("world")
        assertEquals("world", received)
    }
}
