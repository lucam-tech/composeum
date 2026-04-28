package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewDropdownFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val options = listOf("Red", "Green", "Blue")

    @Test
    fun `initial value is displayed`() {
        composeRule.setContent {
            PreviewDropdownField(label = "Color", value = "Red", options = options, onValue = {})
        }
        composeRule.onNodeWithText("Red").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Color").assertExists()
    }

    @Test
    fun `selecting an option fires onValue`() {
        var received = ""
        composeRule.setContent {
            PreviewDropdownField(label = "Color", value = "Red", options = options, onValue = { received = it })
        }
        // Open the dropdown
        composeRule.onNodeWithContentDescription("Color").performClick()
        // Select a different option
        composeRule.onNodeWithText("Blue").performClick()
        assertEquals("Blue", received)
    }
}
