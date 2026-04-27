package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewColorFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `label is displayed`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewColorField(label = "Tint", value = Color.Unspecified, onValue = {})
            }
        }
        composeRule.onNodeWithText("Tint").assertIsDisplayed()
    }

    @Test
    fun `color swatches are rendered`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewColorField(label = "Tint", value = Color.Unspecified, onValue = {})
            }
        }
        composeRule.onNodeWithContentDescription("Primary").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Secondary").assertIsDisplayed()
    }

    @Test
    fun `clicking a swatch fires onValue`() {
        var received: Color? = null
        composeRule.setContent {
            MaterialTheme {
                PreviewColorField(label = "Tint", value = Color.Unspecified, onValue = { received = it })
            }
        }
        composeRule.onNodeWithContentDescription("Primary").performClick()
        assertNotNull(received)
    }
}
