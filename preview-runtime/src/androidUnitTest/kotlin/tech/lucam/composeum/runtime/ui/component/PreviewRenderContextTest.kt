package tech.lucam.composeum.runtime.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewRenderContextTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val group = object : PreviewGroup { override val name = "Test" }

    private fun entry(captureContext: (PreviewRenderContext) -> Unit) = PreviewEntry(
        key = "test/ctx",
        name = "Ctx",
        group = group,
        description = "",
        tags = emptyList(),
        composable = @Composable { captureContext(LocalPreviewRenderContext.current) },
        paramForm = null,
        paramDefaults = PreviewParamDefaults(emptyMap()),
    )

    @Test
    fun `default context has isThumbnail false`() {
        var captured: PreviewRenderContext? = null
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPreviewConfig provides LocalPreviewConfig.current) {
                    captured = LocalPreviewRenderContext.current
                }
            }
        }
        assertFalse(captured!!.isThumbnail)
    }

    @Test
    fun `PreviewThumbnailCard provides isThumbnail true to composable`() {
        var captured: PreviewRenderContext? = null
        composeRule.setContent {
            MaterialTheme {
                PreviewThumbnailCard(
                    entry = entry { captured = it },
                    previewWrapper = null,
                    showTags = false,
                    onClick = {},
                )
            }
        }
        assertTrue(captured!!.isThumbnail)
    }

    @Test
    fun `context outside browser defaults to isThumbnail false`() {
        var captured: PreviewRenderContext? = null
        composeRule.setContent {
            captured = LocalPreviewRenderContext.current
        }
        assertFalse(captured!!.isThumbnail)
    }

    @Test
    fun `explicit isThumbnail false override is respected`() {
        var captured: PreviewRenderContext? = null
        composeRule.setContent {
            CompositionLocalProvider(
                LocalPreviewRenderContext provides PreviewRenderContext(isThumbnail = false),
            ) {
                captured = LocalPreviewRenderContext.current
            }
        }
        assertFalse(captured!!.isThumbnail)
    }
}
