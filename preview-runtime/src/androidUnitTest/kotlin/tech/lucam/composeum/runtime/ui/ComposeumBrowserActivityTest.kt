package tech.lucam.composeum.runtime.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.PreviewParamDefaults
import tech.lucam.composeum.runtime.PreviewRegistry
import tech.lucam.composeum.runtime.config.PreviewConfig
import tech.lucam.composeum.runtime.store.RuntimeSettings
import tech.lucam.composeum.runtime.store.SettingsStorage

@RunWith(RobolectricTestRunner::class)
class ComposeumBrowserActivityTest {

    @get:Rule
    val activityRule = createAndroidComposeRule<ComponentActivity>()

    sealed interface TestGroup : PreviewGroup {
        data object Components : TestGroup {
            override val name = "Components"
        }
    }

    private val registry: PreviewRegistry = object : PreviewRegistry {
        override val entries = listOf(
            PreviewEntry(
                key = "test.SampleButton",
                name = "Sample Button",
                group = TestGroup.Components,
                description = "",
                tags = emptyList(),
                composable = { Text("Sample Button") },
                paramForm = null,
                paramDefaults = PreviewParamDefaults(emptyMap()),
            ),
        )
    }

    private val config = PreviewConfig(
        browserWrapper = { content -> MaterialTheme { content() } },
    )

    private class FakeSettingsStorage(
        initial: RuntimeSettings = RuntimeSettings(),
    ) : SettingsStorage {
        private val state = MutableStateFlow(initial)

        override val settings: Flow<RuntimeSettings> = state

        override suspend fun update(block: RuntimeSettings.() -> RuntimeSettings) {
            state.value = state.value.block()
        }

        override suspend fun reset() {
            state.value = RuntimeSettings()
        }
    }

    @Test
    fun `activity launches and renders browser without crash`() {
        activityRule.activity.setContent {
            ComposeumBrowser(
                registry = registry,
                config = config,
                storage = FakeSettingsStorage(),
            )
        }
        activityRule.onNodeWithText("Compose Preview").assertIsDisplayed()
    }
}
