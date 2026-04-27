package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewBooleanFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `switch reflects initial false value`() {
        composeRule.setContent {
            PreviewBooleanField(label = "Enabled", value = false, onValue = {})
        }
        composeRule.onNodeWithContentDescription("Enabled").assertIsOff()
    }

    @Test
    fun `switch reflects initial true value`() {
        composeRule.setContent {
            PreviewBooleanField(label = "Enabled", value = true, onValue = {})
        }
        composeRule.onNodeWithContentDescription("Enabled").assertIsOn()
    }

    @Test
    fun `clicking switch fires onValue with toggled value`() {
        var received: Boolean? = null
        composeRule.setContent {
            PreviewBooleanField(label = "Enabled", value = false, onValue = { received = it })
        }
        composeRule.onNodeWithContentDescription("Enabled").performClick()
        assertEquals(true, received)
    }
}
