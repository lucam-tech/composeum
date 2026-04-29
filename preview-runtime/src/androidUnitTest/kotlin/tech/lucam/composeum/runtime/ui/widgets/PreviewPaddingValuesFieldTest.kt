package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tech.lucam.composeum.runtime.PreviewPaddingValues

@RunWith(RobolectricTestRunner::class)
class PreviewPaddingValuesFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `label and all side labels are displayed`() {
        composeRule.setContent {
            PreviewPaddingValuesField(
                label = "Padding",
                value = PreviewPaddingValues(),
                onValue = {},
            )
        }
        composeRule.onNodeWithText("Padding").assertIsDisplayed()
        composeRule.onNodeWithText("Top").assertIsDisplayed()
        composeRule.onNodeWithText("Bottom").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsDisplayed()
        composeRule.onNodeWithText("End").assertIsDisplayed()
    }

    @Test
    fun `description is shown when non-empty`() {
        composeRule.setContent {
            PreviewPaddingValuesField(
                label = "Padding",
                value = PreviewPaddingValues(),
                onValue = {},
                description = "Controls spacing",
            )
        }
        composeRule.onNodeWithText("Controls spacing").assertIsDisplayed()
    }
}
