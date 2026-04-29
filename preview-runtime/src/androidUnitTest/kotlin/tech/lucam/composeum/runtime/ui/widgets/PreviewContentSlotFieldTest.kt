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
class PreviewContentSlotFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val names = listOf("Empty", "Icon", "Image")

    @Test
    fun `selected option name is displayed`() {
        composeRule.setContent {
            PreviewContentSlotField(
                label = "Content",
                optionNames = names,
                selectedIndex = 1,
                onIndex = {},
            )
        }
        composeRule.onNodeWithText("Icon").assertIsDisplayed()
    }

    @Test
    fun `selecting an option fires onIndex with correct index`() {
        var received = -1
        composeRule.setContent {
            PreviewContentSlotField(
                label = "Content",
                optionNames = names,
                selectedIndex = 0,
                onIndex = { received = it },
            )
        }
        composeRule.onNodeWithContentDescription("Content").performClick()
        composeRule.onNodeWithText("Image").performClick()
        assertEquals(2, received)
    }

    @Test
    fun `description is shown when non-empty`() {
        composeRule.setContent {
            PreviewContentSlotField(
                label = "Content",
                optionNames = names,
                selectedIndex = 0,
                onIndex = {},
                description = "Pick a slot",
            )
        }
        composeRule.onNodeWithText("Pick a slot").assertIsDisplayed()
    }
}
