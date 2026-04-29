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
class PreviewOptionChipsFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val options = listOf("Small" to 1, "Medium" to 2, "Large" to 3)

    @Test
    fun `all option chips are displayed`() {
        composeRule.setContent {
            PreviewOptionChipsField(label = "Size", value = 1, options = options, onValue = {})
        }
        composeRule.onNodeWithText("Small").assertIsDisplayed()
        composeRule.onNodeWithText("Medium").assertIsDisplayed()
        composeRule.onNodeWithText("Large").assertIsDisplayed()
    }

    @Test
    fun `label is displayed`() {
        composeRule.setContent {
            PreviewOptionChipsField(label = "Size", value = 1, options = options, onValue = {})
        }
        composeRule.onNodeWithText("Size").assertIsDisplayed()
    }

    @Test
    fun `tapping a chip fires onValue with its value`() {
        var received: Int? = null
        composeRule.setContent {
            PreviewOptionChipsField(
                label = "Size",
                value = 1,
                options = options,
                onValue = { received = it },
            )
        }
        composeRule.onNodeWithContentDescription("Size: Large").performClick()
        assertEquals(3, received)
    }

    @Test
    fun `description is shown when non-empty`() {
        composeRule.setContent {
            PreviewOptionChipsField(
                label = "Size",
                value = 1,
                options = options,
                onValue = {},
                description = "Pick a size",
            )
        }
        composeRule.onNodeWithText("Pick a size").assertIsDisplayed()
    }
}
