package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewListFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `item count is displayed`() {
        composeRule.setContent {
            PreviewListField(label = "Items", itemCount = 3, onAdd = {}, onRemove = {}) { _ -> }
        }
        composeRule.onNodeWithText("3 items").assertIsDisplayed()
    }

    @Test
    fun `label is displayed`() {
        composeRule.setContent {
            PreviewListField(label = "Tags", itemCount = 0, onAdd = {}, onRemove = {}) { _ -> }
        }
        composeRule.onNodeWithText("Tags").assertIsDisplayed()
    }

    @Test
    fun `item content is rendered for each index`() {
        composeRule.setContent {
            PreviewListField(label = "Items", itemCount = 2, onAdd = {}, onRemove = {}) { index ->
                Text("item-$index")
            }
        }
        composeRule.onNodeWithText("item-0").assertIsDisplayed()
        composeRule.onNodeWithText("item-1").assertIsDisplayed()
    }

    @Test
    fun `onAdd fires when plus button is clicked`() {
        var addClicked = false
        composeRule.setContent {
            PreviewListField(label = "Items", itemCount = 0, onAdd = { addClicked = true }, onRemove = {}) { _ -> }
        }
        composeRule.onNodeWithText("+").performClick()
        assertEquals(true, addClicked)
    }

    @Test
    fun `description is shown when provided`() {
        composeRule.setContent {
            PreviewListField(label = "Items", itemCount = 0, onAdd = {}, onRemove = {}, description = "hint") { _ -> }
        }
        composeRule.onNodeWithText("hint").assertIsDisplayed()
    }
}
