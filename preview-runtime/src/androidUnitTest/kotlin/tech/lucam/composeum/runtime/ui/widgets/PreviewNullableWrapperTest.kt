package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewNullableWrapperTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `content is hidden when isNull is true`() {
        composeRule.setContent {
            PreviewNullableWrapper(label = "Value", isNull = true, onNullChange = {}) {
                Text("inner content")
            }
        }
        composeRule.onNodeWithText("inner content").assertIsNotDisplayed()
    }

    @Test
    fun `content is shown when isNull is false`() {
        composeRule.setContent {
            PreviewNullableWrapper(label = "Value", isNull = false, onNullChange = {}) {
                Text("inner content")
            }
        }
        composeRule.onNodeWithText("inner content").assertIsDisplayed()
    }

    @Test
    fun `label is displayed`() {
        composeRule.setContent {
            PreviewNullableWrapper(label = "Title", isNull = true, onNullChange = {}) {}
        }
        composeRule.onNodeWithText("Title (nullable)").assertIsDisplayed()
    }

    @Test
    fun `toggling switch fires onNullChange`() {
        var received: Boolean? = null
        composeRule.setContent {
            PreviewNullableWrapper(label = "Value", isNull = true, onNullChange = { received = it }) {}
        }
        composeRule.onNodeWithText("null").performClick()
        // The switch toggles from "is null" (checked = false in the switch) to non-null
        assertEquals(false, received)
    }
}
