package tech.lucam.composeum.ksp

import tech.lucam.composeum.ksp.model.ModelBuilder
import tech.lucam.composeum.ksp.model.PreviewModel
import tech.lucam.composeum.ksp.validation.Validator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModelBuilderTest {

    // Populated by the capturing processor during compilation; accessible because
    // kotlin-compile-testing runs processors in the same JVM as the test.
    private val capturedModels = mutableListOf<PreviewModel>()

    @Before
    fun setUp() {
        capturedModels.clear()
    }

    private inner class CapturingProcessor(
        private val env: SymbolProcessorEnvironment,
    ) : SymbolProcessor {
        override fun process(resolver: Resolver): List<KSAnnotated> {
            resolver
                .getSymbolsWithAnnotation("tech.lucam.composeum.annotation.ComposePreview")
                .filterIsInstance<KSFunctionDeclaration>()
                .filter { Validator.validate(it, env.logger, resolver, strictTypes = true) }
                .mapTo(capturedModels) { ModelBuilder.build(it, enableKdoc = true) }
            return emptyList()
        }
    }

    private inner class CapturingProcessorProvider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
            CapturingProcessor(environment)
    }

    private val composableStub = SourceFile.kotlin(
        "Composable.kt",
        """
        package androidx.compose.runtime
        annotation class Composable
        """,
    )
    private val previewGroupStub = SourceFile.kotlin(
        "PreviewGroup.kt",
        """
        package tech.lucam.composeum.annotation
        interface PreviewGroup { val name: String; val description: String get() = "" }
        """,
    )
    private val previewParamStub = SourceFile.kotlin(
        "PreviewParam.kt",
        """
        package tech.lucam.composeum.annotation
        annotation class PreviewParam(
            val label: String,
            val default: String = "",
            val description: String = "",
            val options: Array<String> = [],
        )
        """,
    )
    private val composePreviewStub = SourceFile.kotlin(
        "ComposePreview.kt",
        """
        package tech.lucam.composeum.annotation
        annotation class ComposePreview(
            val name: String = "",
            val group: kotlin.reflect.KClass<*> = PreviewGroup::class,
            val description: String = "",
            val tags: Array<String> = [],
        )
        """,
    )

    private fun compile(vararg sources: SourceFile): KotlinCompilation.Result =
        KotlinCompilation().apply {
            this.sources = listOf(
                composableStub,
                previewGroupStub,
                previewParamStub,
                composePreviewStub,
            ) + sources.toList()
            symbolProcessorProviders = listOf(CapturingProcessorProvider())
            inheritClassPath = false
        }.compile()

    @Test
    fun `builds correct model for String param`() {
        compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "Components" }

                @ComposePreview(name = "Button Preview", group = MyGroup::class, description = "A button")
                @Composable
                fun buttonPreview(
                    @PreviewParam(label = "Label", default = "Click me") text: String = "Click me",
                ) {}
                """,
            ),
        )

        assertEquals(1, capturedModels.size)
        val model = capturedModels[0]
        assertEquals("com.example.buttonPreview", model.key)
        assertEquals("Button Preview", model.name)
        assertEquals("A button", model.description)
        assertEquals("MyGroup", model.groupExpression)
        assertEquals("com.example.MyGroup", model.groupImport)
        assertEquals(1, model.params.size)
        val param = model.params[0]
        assertEquals("text", param.name)
        assertEquals("Label", param.label)
        assertEquals("kotlin.String", param.kotlinType)
        assertEquals("Click me", param.defaultValue)
        assertTrue(param.options.isEmpty())
    }

    @Test
    fun `builds correct model for Boolean param`() {
        compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "Components" }

                @ComposePreview(name = "Toggle", group = MyGroup::class)
                @Composable
                fun togglePreview(
                    @PreviewParam(label = "Enabled", default = "true") enabled: Boolean = true,
                ) {}
                """,
            ),
        )

        assertEquals(1, capturedModels.size)
        val param = capturedModels[0].params[0]
        assertEquals("enabled", param.name)
        assertEquals("Enabled", param.label)
        assertEquals("kotlin.Boolean", param.kotlinType)
        assertEquals("true", param.defaultValue)
    }

    @Test
    fun `builds correct model for Enum param`() {
        compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "Components" }
                enum class ButtonSize { SMALL, MEDIUM, LARGE }

                @ComposePreview(name = "Sized Button", group = MyGroup::class)
                @Composable
                fun sizedButtonPreview(
                    @PreviewParam(label = "Size") size: ButtonSize = ButtonSize.MEDIUM,
                ) {}
                """,
            ),
        )

        assertEquals(1, capturedModels.size)
        val param = capturedModels[0].params[0]
        assertEquals("size", param.name)
        assertEquals("Size", param.label)
        assertEquals("com.example.ButtonSize", param.kotlinType)
        assertTrue(param.options.isEmpty())
    }

    @Test
    fun `builds correct model for param with options`() {
        compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "Components" }

                @ComposePreview(name = "Themed Button", group = MyGroup::class)
                @Composable
                fun themedButtonPreview(
                    @PreviewParam(label = "Theme", options = ["light", "dark", "contrast"])
                    theme: String = "light",
                ) {}
                """,
            ),
        )

        assertEquals(1, capturedModels.size)
        val param = capturedModels[0].params[0]
        assertEquals("theme", param.name)
        assertEquals("Theme", param.label)
        assertEquals(listOf("light", "dark", "contrast"), param.options)
    }

    @Test
    fun `falls back to function name and synthetic top level group when name and group are omitted`() {
        compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview

                @ComposePreview
                @Composable
                fun fallbackPreview() {}
                """,
            ),
        )

        assertEquals(1, capturedModels.size)
        val model = capturedModels[0]
        assertEquals("fallbackPreview", model.name)
        assertEquals("GeneratedComposePreviewTopLevelGroup", model.groupExpression)
        assertEquals("", model.groupImport)
        assertEquals("", model.syntheticGroupDisplayName)
    }

    @Test
    fun `uses KDoc summary and param docs as fallbacks`() {
        compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "Components" }

                /**
                 * Button shown in its default resting state.
                 *
                 * Additional details that should not be included in the summary.
                 *
                 * @param text Label shown on the button.
                 */
                @ComposePreview(name = "Button", group = MyGroup::class)
                @Composable
                fun buttonPreview(
                    @PreviewParam(label = "Text") text: String = "Click me",
                ) {}
                """,
            ),
        )

        assertEquals(1, capturedModels.size)
        val model = capturedModels[0]
        assertEquals("Button shown in its default resting state.", model.description)
        assertEquals("Label shown on the button.", model.params.single().description)
    }

    @Test
    fun `explicit descriptions override KDoc fallbacks`() {
        compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "Components" }

                /**
                 * KDoc summary.
                 *
                 * @param text KDoc param description.
                 */
                @ComposePreview(name = "Button", group = MyGroup::class, description = "Explicit preview description")
                @Composable
                fun buttonPreview(
                    @PreviewParam(label = "Text", description = "Explicit param description")
                    text: String = "Click me",
                ) {}
                """,
            ),
        )

        assertEquals(1, capturedModels.size)
        val model = capturedModels[0]
        assertEquals("Explicit preview description", model.description)
        assertEquals("Explicit param description", model.params.single().description)
    }
}
