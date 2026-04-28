package tech.lucam.composeum.runtime.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewThumbnailCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val entry = PreviewEntry(
        key = "test.thumbnail",
        name = "Card Name",
        group = object : PreviewGroup { override val name = "Components" },
        description = "",
        tags = emptyList(),
        composable = { Text("Inner preview") },
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    @Test
    fun `thumbnail hides preview semantics tree from accessibility`() {
        composeRule.setContent {
            MaterialTheme {
                PreviewThumbnailCard(
                    entry = entry,
                    previewWrapper = null,
                    showTags = false,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Card Name").assertIsDisplayed()
        composeRule.onNodeWithText("Inner preview").assertDoesNotExist()
    }
}
