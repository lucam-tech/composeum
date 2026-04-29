package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.ui.Alignment
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewAlignmentFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `all nine alignment cells are present`() {
        composeRule.setContent {
            PreviewAlignmentField(label = "Align", value = Alignment.Center, onValue = {})
        }
        listOf(
            "Top Start", "Top Center", "Top End",
            "Center Start", "Center", "Center End",
            "Bottom Start", "Bottom Center", "Bottom End",
        ).forEach { name ->
            composeRule.onNodeWithContentDescription("Align: $name").assertExists()
        }
    }

    @Test
    fun `tapping a cell fires onValue with correct alignment`() {
        var received: Alignment? = null
        composeRule.setContent {
            PreviewAlignmentField(
                label = "Align",
                value = Alignment.Center,
                onValue = { received = it },
            )
        }
        composeRule.onNodeWithContentDescription("Align: Top Start").performClick()
        assertEquals(Alignment.TopStart, received)
    }

    @Test
    fun `tapping bottom end fires onValue with BottomEnd`() {
        var received: Alignment? = null
        composeRule.setContent {
            PreviewAlignmentField(
                label = "Pos",
                value = Alignment.TopStart,
                onValue = { received = it },
            )
        }
        composeRule.onNodeWithContentDescription("Pos: Bottom End").performClick()
        assertEquals(Alignment.BottomEnd, received)
    }
}
