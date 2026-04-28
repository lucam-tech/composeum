package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewTextUnitFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `initial value is displayed with sp suffix`() {
        composeRule.setContent {
            PreviewTextUnitField(label = "Font Size", value = 16.sp, onValue = {})
        }
        composeRule.onNodeWithText("16sp").assertIsDisplayed()
    }

    @Test
    fun `label is displayed`() {
        composeRule.setContent {
            PreviewTextUnitField(label = "Font Size", value = 14.sp, onValue = {})
        }
        composeRule.onNodeWithText("Font Size").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Font Size").assertExists()
    }

    @Test
    fun `description is shown when provided`() {
        composeRule.setContent {
            PreviewTextUnitField(label = "Font Size", value = 14.sp, onValue = {}, description = "sp unit")
        }
        composeRule.onNodeWithText("sp unit").assertIsDisplayed()
    }
}
