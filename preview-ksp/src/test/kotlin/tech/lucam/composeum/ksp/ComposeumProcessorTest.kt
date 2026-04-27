package tech.lucam.composeum.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ComposeumProcessorTest {

    // Stubs for types not on the JVM test classpath.
    private val runtimeStubs = SourceFile.kotlin(
        "Runtime.kt",
        """
        package tech.lucam.composeum.runtime
        import androidx.compose.runtime.Composable
        import tech.lucam.composeum.annotation.PreviewGroup

        data class PreviewParamState(val values: Map<String, Any> = emptyMap()) {
            fun put(key: String, value: Any): PreviewParamState = copy(values = values + (key to value))
            @Suppress("UNCHECKED_CAST")
            operator fun <T> get(key: String): T? = values[key] as? T
        }
        data class PreviewParamDefaults(val defaults: Map<String, Any>)
        data class PreviewEntry(
            val key: String,
            val name: String,
            val group: PreviewGroup,
            val description: String,
            val tags: List<String>,
            val composable: @Composable () -> Unit,
            val paramForm: (@Composable (PreviewParamState, (PreviewParamState) -> Unit) -> Unit)?,
            val paramDefaults: PreviewParamDefaults,
            val sourceFile: String = "",
            val sourceLine: Int = 0,
        )
        interface PreviewRegistry { val entries: List<PreviewEntry> }
        """,
    )

    private val widgetStubs = SourceFile.kotlin(
        "Widgets.kt",
        """
        package tech.lucam.composeum.runtime.ui.widgets
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.graphics.Color
        import androidx.compose.ui.unit.Dp
        import androidx.compose.ui.unit.TextUnit

        @Composable fun PreviewStringField(label: String, value: String, onValue: (String) -> Unit, description: String = "") {}
        @Composable fun PreviewBooleanField(label: String, value: Boolean, onValue: (Boolean) -> Unit, description: String = "") {}
        @Composable fun PreviewIntField(label: String, value: Int, onValue: (Int) -> Unit, range: IntRange = 0..100, description: String = "") {}
        @Composable fun PreviewFloatField(label: String, value: Float, onValue: (Float) -> Unit, range: ClosedFloatingPointRange<Float> = 0f..1f, description: String = "") {}
        @Composable fun PreviewDropdownField(label: String, value: String, options: List<String>, onValue: (String) -> Unit, description: String = "") {}
        @Composable fun PreviewColorField(label: String, value: Color, onValue: (Color) -> Unit, description: String = "") {}
        @Composable fun PreviewDpField(label: String, value: Dp, onValue: (Dp) -> Unit, range: ClosedFloatingPointRange<Float> = 0f..512f, description: String = "") {}
        @Composable fun PreviewTextUnitField(label: String, value: TextUnit, onValue: (TextUnit) -> Unit, range: ClosedFloatingPointRange<Float> = 8f..64f, description: String = "") {}
        @Composable fun PreviewNullableWrapper(label: String, isNull: Boolean, onNullChange: (Boolean) -> Unit, description: String = "", content: @Composable () -> Unit) {}
        @Composable fun PreviewListField(label: String, itemCount: Int, onAdd: () -> Unit, onRemove: (Int) -> Unit, description: String = "", itemContent: @Composable (Int) -> Unit) {}
        """,
    )

    private val colorStub = SourceFile.kotlin(
        "Color.kt",
        """
        package androidx.compose.ui.graphics
        class Color(val value: ULong) {
            companion object {
                val Unspecified = Color(0uL)
                val White = Color(0xFFFFFFFFuL)
                val Black = Color(0xFF000000uL)
            }
        }
        // Top-level factory matching Compose's fun Color(@ColorInt color: Int): Color
        fun Color(color: Int): Color = Color((color.toLong() and 0xffffffffL).toULong())
        """,
    )

    private val dpAndTextUnitStub = SourceFile.kotlin(
        "DpTextUnit.kt",
        """
        package androidx.compose.ui.unit
        @kotlin.jvm.JvmInline
        value class Dp(val value: Float) {
            companion object { val Unspecified = Dp(Float.NaN) }
        }
        enum class TextUnitType { Unspecified, Sp, Em }
        @kotlin.jvm.JvmInline
        value class TextUnit(val packedValue: Long) {
            val isSp: Boolean get() = true
            val value: Float get() = java.lang.Float.intBitsToFloat((packedValue shr 32).toInt())
            companion object { val Unspecified = TextUnit(0L) }
        }
        fun TextUnit(value: Float, type: TextUnitType): TextUnit =
            TextUnit((java.lang.Float.floatToRawIntBits(value).toLong() shl 32) or type.ordinal.toLong())
        val Float.sp: TextUnit get() = TextUnit(this, TextUnitType.Sp)
        val Int.dp: Dp get() = Dp(this.toFloat())
        """,
    )

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
        interface PreviewGroup {
            val name: String
            val description: String get() = ""
        }
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
    // Loose group type (KClass<*>) so we can pass non-PreviewGroup classes for rule-2 test.
    private val composePreviewStub = SourceFile.kotlin(
        "ComposePreview.kt",
        """
        package tech.lucam.composeum.annotation
        annotation class ComposePreview(
            val name: String,
            val group: kotlin.reflect.KClass<*>,
            val description: String = "",
            val tags: Array<String> = [],
        )
        """,
    )
    private val viewPreviewStub = SourceFile.kotlin(
        "ViewPreview.kt",
        """
        package tech.lucam.composeum.annotation
        annotation class ViewPreview(
            val name: String,
            val group: kotlin.reflect.KClass<*>,
            val description: String = "",
            val tags: Array<String> = [],
        )
        """,
    )
    // Split into separate files — Kotlin only allows one package declaration per file.
    private val androidViewStub = SourceFile.kotlin(
        "AndroidView.kt",
        """
        package android.view
        open class View
        """,
    )
    private val androidContextStub = SourceFile.kotlin(
        "AndroidContext.kt",
        """
        package android.content
        open class Context
        """,
    )
    private val androidViewInteropStub = SourceFile.kotlin(
        "AndroidViewInterop.kt",
        """
        package androidx.compose.ui.viewinterop
        import androidx.compose.runtime.Composable
        @Composable fun <T : android.view.View> AndroidView(factory: (android.content.Context) -> T) {}
        """,
    )
    private val androidViewStubs get() = listOf(androidViewStub, androidContextStub, androidViewInteropStub)

    private val androidPreviewStub = SourceFile.kotlin(
        "AndroidPreview.kt",
        """
        package androidx.compose.ui.tooling.preview
        annotation class Preview(
            val name: String = "",
            val group: String = "",
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
            symbolProcessorProviders = listOf(ComposeumProcessorProvider())
            inheritClassPath = false
        }.compile()

    @Test
    fun `error when ComposePreview applied to non-Composable function`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "Group" }

                @ComposePreview(name = "Test", group = MyGroup::class)
                fun notComposable() {}
                """,
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@ComposePreview can only be applied to @Composable functions"))
    }

    @Test
    fun `error when ComposePreview applied to extension function`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "Group" }

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun String.extensionPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@ComposePreview functions must not be extension functions"))
    }

    @Test
    fun `error when ComposePreview applied to generic function`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "Group" }

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun <T> genericPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@ComposePreview functions must not declare type parameters"))
    }

    @Test
    fun `error when ComposePreview applied to suspend function`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "Group" }

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                suspend fun suspendPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@ComposePreview functions must not be suspend"))
    }

    @Test
    fun `error when group does not implement PreviewGroup`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview

                object NotAGroup

                @ComposePreview(name = "Test", group = NotAGroup::class)
                @Composable
                fun myPreview() {}
                """,
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "@ComposePreview group 'NotAGroup' must implement PreviewGroup",
            ),
        )
    }

    @Test
    fun `error when PreviewParam parameter has no default value`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup
                import tech.lucam.composeum.annotation.PreviewParam

                object MyGroup : PreviewGroup { override val name = "Group" }

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun myPreview(@PreviewParam(label = "Text") text: String) {}
                """,
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "@PreviewParam parameter 'text' must have a default value in the function signature",
            ),
        )
    }

    @Test
    fun `multiple ComposePreview violations are all reported`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewParam

                object NotAGroup

                @ComposePreview(name = "Broken", group = NotAGroup::class)
                @Composable
                fun String.brokenPreview(
                    @PreviewParam(label = "Text") text: String,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@ComposePreview functions must not be extension functions"))
        assertTrue(result.messages.contains("@ComposePreview group 'NotAGroup' must implement PreviewGroup"))
        assertTrue(
            result.messages.contains(
                "@PreviewParam parameter 'text' must have a default value in the function signature",
            ),
        )
    }

    @Test
    fun `custom type emits warning in strict mode but compiles successfully`() {
        // Custom types (not enum, data class, sealed, or known primitive) are supported
        // via PreviewConfig.customTypeFields. A warning is emitted to remind the developer
        // to register a CustomParamField; compilation succeeds so the generated code can
        // be wired up at runtime.
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup
                import tech.lucam.composeum.annotation.PreviewParam

                object MyGroup : PreviewGroup { override val name = "Group" }
                class MyCustomType(val value: Int)

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Custom") custom: MyCustomType = MyCustomType(0),
                ) {}
                """,
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("@PreviewParam parameter 'custom' uses custom type 'MyCustomType'"))
        assertTrue(result.messages.contains("customTypeField<MyCustomType>"))
    }

    @Test
    fun `valid function with supported param type passes validation`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup
                import tech.lucam.composeum.annotation.PreviewParam

                object MyGroup : PreviewGroup { override val name = "Group" }

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Text") text: String = "hello",
                    @PreviewParam(label = "Flag") flag: Boolean = true,
                ) {}
                """,
            ),
        )
        // Verify no KSP validation errors — compilation of generated code may fail until TASK-009.
        assertTrue(!result.messages.contains("error: @ComposePreview") && !result.messages.contains("error: @PreviewParam"))
    }

    // Helper that keeps the KotlinCompilation object so callers can inspect generated sources.
    private fun compileRetaining(vararg sources: SourceFile): Pair<KotlinCompilation.Result, KotlinCompilation> {
        val compilation = KotlinCompilation().apply {
            this.sources = listOf(
                composableStub,
                colorStub,
                dpAndTextUnitStub,
                previewGroupStub,
                previewParamStub,
                composePreviewStub,
                runtimeStubs,
                widgetStubs,
            ) + sources.toList()
            symbolProcessorProviders = listOf(ComposeumProcessorProvider())
            inheritClassPath = false
        }
        return compilation.compile() to compilation
    }

    private fun findGeneratedFile(compilation: KotlinCompilation, name: String): File? =
        compilation.workingDir
            .walkTopDown()
            .firstOrNull { it.isFile && it.name == "$name.kt" }

    @Test
    fun `unsupported type is allowed when options provided`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup
                import tech.lucam.composeum.annotation.PreviewParam

                object MyGroup : PreviewGroup { override val name = "Group" }
                data class MyCustomType(val value: Int)

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Custom", options = ["a", "b"])
                    custom: MyCustomType = MyCustomType(0),
                ) {}
                """,
            ),
        )
        // Verify no KSP validation errors — compilation of generated code may fail until TASK-009.
        assertTrue(!result.messages.contains("error: @ComposePreview") && !result.messages.contains("error: @PreviewParam"))
    }

    // ── Registry generation tests ─────────────────────────────────────────────

    @Test
    fun `registry generated for single preview with no params`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "Components" }

                @ComposePreview(name = "My Preview", group = MyGroup::class, description = "A test")
                @Composable
                fun myPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()

        assertTrue(content.contains("key = \"com.example.myPreview\""))
        assertTrue(content.contains("name = \"My Preview\""))
        assertTrue(content.contains("description = \"A test\""))
        assertTrue(content.contains("group = MyGroup"))
        assertTrue(content.contains("composable = { myPreview() }"))
        assertTrue(content.contains("paramForm = null"))
        assertTrue(content.contains("paramDefaults = PreviewParamDefaults(defaults = emptyMap())"))
        assertTrue(content.contains(": PreviewRegistry"))
        assertTrue(content.contains("sourceFile ="))
        assertTrue(content.contains("sourceLine ="))
    }

    @Test
    fun `registry references param form and typed defaults when preview has params`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "Components" }

                @ComposePreview(name = "Button", group = MyGroup::class)
                @Composable
                fun buttonPreview(
                    @PreviewParam(label = "Label", default = "Click me") text: String = "Click me",
                    @PreviewParam(label = "Enabled", default = "true") enabled: Boolean = true,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()

        assertTrue(content.contains("key = \"com.example.buttonPreview\""))
        assertTrue(content.contains("paramForm = { state, onUpdate -> buttonPreviewParamForm(state, onUpdate) }"))
        assertTrue(content.contains("\"text\" to \"Click me\""))
        assertTrue(content.contains("\"enabled\" to true"))
    }

    // ── Param form generation tests ───────────────────────────────────────────

    @Test
    fun `param form emits PreviewStringField for String param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Title", default = "Hello") title: String = "Hello",
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("PreviewStringField"))
        assertTrue(content.contains("state[\"title\"] ?: \"Hello\""))
        assertTrue(content.contains("onUpdate(state.put(\"title\", it))"))
    }

    @Test
    fun `param form emits PreviewBooleanField for Boolean param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Enabled", default = "true") enabled: Boolean = true,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("PreviewBooleanField"))
        assertTrue(content.contains("state[\"enabled\"] ?: true"))
    }

    @Test
    fun `param form emits PreviewIntField for Int param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Count", default = "5") count: Int = 5,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("PreviewIntField"))
        assertTrue(content.contains("state[\"count\"] ?: 5"))
    }

    @Test
    fun `param form emits PreviewFloatField for Float param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Alpha", default = "0.5") alpha: Float = 0.5f,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("PreviewFloatField"))
        assertTrue(content.contains("state[\"alpha\"] ?: 0.5f"))
    }

    @Test
    fun `param form emits PreviewDropdownField for Enum param with enum values as options`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }
                enum class ButtonSize { SMALL, MEDIUM, LARGE }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Size") size: ButtonSize = ButtonSize.MEDIUM,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("PreviewDropdownField"))
        assertTrue(content.contains("\"SMALL\""))
        assertTrue(content.contains("\"MEDIUM\""))
        assertTrue(content.contains("\"LARGE\""))
    }

    @Test
    fun `param form emits PreviewDropdownField for param with explicit options`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Theme", options = ["light", "dark", "high-contrast"])
                    theme: String = "light",
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("PreviewDropdownField"))
        assertTrue(content.contains("\"light\""))
        assertTrue(content.contains("\"dark\""))
        assertTrue(content.contains("\"high-contrast\""))
    }

    @Test
    fun `registry contains entries for two previews in different groups`() {
        val (_, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object ComponentsGroup : PreviewGroup { override val name = "Components" }
                object ScreensGroup : PreviewGroup { override val name = "Screens" }

                @ComposePreview(name = "Button", group = ComponentsGroup::class)
                @Composable
                fun buttonPreview() {}

                @ComposePreview(name = "Home Screen", group = ScreensGroup::class)
                @Composable
                fun homeScreenPreview() {}
                """,
            ),
        )

        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()

        assertTrue(content.contains("key = \"com.example.buttonPreview\""))
        assertTrue(content.contains("key = \"com.example.homeScreenPreview\""))
        assertTrue(content.contains("group = ComponentsGroup"))
        assertTrue(content.contains("group = ScreensGroup"))
    }

    // ── CMP-002: @ViewPreview + KMP source set verification ──────────────────

    @Test
    fun `ViewPreview with context param generates AndroidView factory with it`() {
        val (result, compilation) = compileRetainingWithView(
            SourceFile.kotlin(
                "XmlPreview.kt",
                """
                package com.example
                import android.content.Context
                import android.view.View
                import tech.lucam.composeum.annotation.ViewPreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ViewPreview(name = "My View", group = MyGroup::class)
                fun myViewPreview(context: Context): View = View()
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()
        assertTrue("Registry should contain the view preview key", content.contains("myViewPreview"))
        assertTrue("Composable lambda should use AndroidView", content.contains("AndroidView"))
        assertTrue("Factory lambda should forward context via 'it'", content.contains("myViewPreview(it)"))
    }

    @Test
    fun `ViewPreview without context param generates AndroidView factory ignoring context`() {
        val (result, compilation) = compileRetainingWithView(
            SourceFile.kotlin(
                "XmlPreview.kt",
                """
                package com.example
                import android.view.View
                import tech.lucam.composeum.annotation.ViewPreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ViewPreview(name = "No Context View", group = MyGroup::class)
                fun myViewPreview(): View = View()
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()
        assertTrue("Composable lambda should use AndroidView", content.contains("AndroidView"))
        assertTrue("Factory lambda should call function with no args", content.contains("myViewPreview()"))
    }

    @Test
    fun `error when ViewPreview applied to Composable function`() {
        val result = compileWithView(
            SourceFile.kotlin(
                "XmlPreview.kt",
                """
                package com.example
                import android.view.View
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ViewPreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ViewPreview(name = "Bad", group = MyGroup::class)
                @Composable
                fun badViewPreview(): View = View()
                """,
            ),
        )
        assertTrue("Should error on @Composable @ViewPreview", result.messages.contains("@ViewPreview functions must not be @Composable"))
    }

    @Test
    fun `error when ViewPreview applied to extension function`() {
        val result = compileWithView(
            SourceFile.kotlin(
                "XmlPreview.kt",
                """
                package com.example
                import android.view.View
                import tech.lucam.composeum.annotation.ViewPreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ViewPreview(name = "Bad", group = MyGroup::class)
                fun String.badViewPreview(): View = View()
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@ViewPreview functions must not be extension functions"))
    }

    @Test
    fun `error when ViewPreview applied to generic function`() {
        val result = compileWithView(
            SourceFile.kotlin(
                "XmlPreview.kt",
                """
                package com.example
                import android.view.View
                import tech.lucam.composeum.annotation.ViewPreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ViewPreview(name = "Bad", group = MyGroup::class)
                fun <T> badViewPreview(): View = View()
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@ViewPreview functions must not declare type parameters"))
    }

    @Test
    fun `error when ViewPreview applied to suspend function`() {
        val result = compileWithView(
            SourceFile.kotlin(
                "XmlPreview.kt",
                """
                package com.example
                import android.view.View
                import tech.lucam.composeum.annotation.ViewPreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ViewPreview(name = "Bad", group = MyGroup::class)
                suspend fun badViewPreview(): View = View()
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@ViewPreview functions must not be suspend"))
    }

    @Test
    fun `error when ViewPreview function does not return View`() {
        val result = compileWithView(
            SourceFile.kotlin(
                "XmlPreview.kt",
                """
                package com.example
                import tech.lucam.composeum.annotation.ViewPreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ViewPreview(name = "Bad", group = MyGroup::class)
                fun badViewPreview(): String = "not a view"
                """,
            ),
        )
        assertTrue("Should error on non-View return type", result.messages.contains("must return android.view.View"))
    }

    @Test
    fun `error when ViewPreview group does not implement PreviewGroup`() {
        val result = compileWithView(
            SourceFile.kotlin(
                "XmlPreview.kt",
                """
                package com.example
                import android.view.View
                import tech.lucam.composeum.annotation.ViewPreview

                object NotAGroup

                @ViewPreview(name = "Bad", group = NotAGroup::class)
                fun badViewPreview(): View = View()
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "@ViewPreview group 'com.example.NotAGroup' must implement PreviewGroup",
            ),
        )
    }

    @Test
    fun `ComposePreview and ViewPreview are both emitted in the same registry`() {
        val (result, compilation) = compileRetainingWithView(
            SourceFile.kotlin(
                "Previews.kt",
                """
                package com.example
                import android.view.View
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.ViewPreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "Compose Preview", group = MyGroup::class)
                @Composable
                fun myComposePreview() {}

                @ViewPreview(name = "View Preview", group = MyGroup::class)
                fun myViewPreview(): View = View()
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("myComposePreview"))
        assertTrue(content.contains("myViewPreview"))
        assertTrue(content.contains("AndroidView"))
    }

    private fun compileWithView(vararg sources: SourceFile): KotlinCompilation.Result =
        KotlinCompilation().apply {
            this.sources = listOf(
                composableStub,
                previewGroupStub,
                previewParamStub,
                composePreviewStub,
                viewPreviewStub,
            ) + androidViewStubs + sources.toList()
            symbolProcessorProviders = listOf(ComposeumProcessorProvider())
            inheritClassPath = false
        }.compile()

    private fun compileRetainingWithView(vararg sources: SourceFile): Pair<KotlinCompilation.Result, KotlinCompilation> {
        val compilation = KotlinCompilation().apply {
            this.sources = listOf(
                composableStub,
                colorStub,
                previewGroupStub,
                previewParamStub,
                composePreviewStub,
                viewPreviewStub,
            ) + androidViewStubs + listOf(
                runtimeStubs,
                widgetStubs,
            ) + sources.toList()
            symbolProcessorProviders = listOf(ComposeumProcessorProvider())
            inheritClassPath = false
        }
        return compilation.compile() to compilation
    }

    // ── @Preview (Jetpack Compose built-in) support ───────────────────────────

    private fun compileWithAndroidPreview(
        vararg sources: SourceFile,
        includeAndroidPreview: Boolean = true,
    ): KotlinCompilation.Result =
        KotlinCompilation().apply {
            this.sources = listOf(
                composableStub,
                previewGroupStub,
                previewParamStub,
                composePreviewStub,
                androidPreviewStub,
            ) + sources.toList()
            symbolProcessorProviders = listOf(ComposeumProcessorProvider())
            kspArgs = mutableMapOf("composeum.includeAndroidPreview" to includeAndroidPreview.toString())
            inheritClassPath = false
        }.compile()

    private fun compileRetainingWithAndroidPreview(vararg sources: SourceFile): Pair<KotlinCompilation.Result, KotlinCompilation> {
        val compilation = KotlinCompilation().apply {
            this.sources = listOf(
                composableStub,
                colorStub,
                previewGroupStub,
                previewParamStub,
                composePreviewStub,
                androidPreviewStub,
                runtimeStubs,
                widgetStubs,
            ) + sources.toList()
            symbolProcessorProviders = listOf(ComposeumProcessorProvider())
            kspArgs = mutableMapOf("composeum.includeAndroidPreview" to "true")
            inheritClassPath = false
        }
        return compilation.compile() to compilation
    }

    @Test
    fun `@Preview function is ignored when includeAndroidPreview is false`() {
        val result = KotlinCompilation().apply {
            this.sources = listOf(
                composableStub,
                previewGroupStub,
                previewParamStub,
                composePreviewStub,
                androidPreviewStub,
                SourceFile.kotlin(
                    "Preview.kt",
                    """
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.tooling.preview.Preview

                    @Preview(name = "Button")
                    @Composable
                    fun buttonPreview() {}
                    """,
                ),
            )
            symbolProcessorProviders = listOf(ComposeumProcessorProvider())
            // includeAndroidPreview NOT set — defaults to false
            inheritClassPath = false
        }.compile()
        // No registry generated since no symbols were collected
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `@Preview function with name and group is emitted in registry`() {
        val (result, compilation) = compileRetainingWithAndroidPreview(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.tooling.preview.Preview

                @Preview(name = "My Button", group = "Buttons")
                @Composable
                fun buttonPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()

        assertTrue(content.contains("key = \"com.example.buttonPreview\""))
        assertTrue(content.contains("name = \"My Button\""))
        assertTrue(content.contains("GeneratedAndroidPreviewGroup_Buttons"))
        assertTrue(content.contains("\"Buttons\""))
        assertTrue(content.contains("paramForm = null"))
    }

    @Test
    fun `error when @Preview applied to extension function`() {
        val result = compileWithAndroidPreview(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.tooling.preview.Preview

                @Preview(name = "Bad")
                @Composable
                fun String.badPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@Preview functions must not be extension functions"))
    }

    @Test
    fun `error when @Preview applied to generic function`() {
        val result = compileWithAndroidPreview(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.tooling.preview.Preview

                @Preview(name = "Bad")
                @Composable
                fun <T> badPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@Preview functions must not declare type parameters"))
    }

    @Test
    fun `error when @Preview applied to suspend function`() {
        val result = compileWithAndroidPreview(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.tooling.preview.Preview

                @Preview(name = "Bad")
                @Composable
                suspend fun badPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@Preview functions must not be suspend"))
    }

    @Test
    fun `@Preview with empty name falls back to function name`() {
        val (result, compilation) = compileRetainingWithAndroidPreview(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.tooling.preview.Preview

                @Preview
                @Composable
                fun cardPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("name = \"cardPreview\""))
    }

    @Test
    fun `@Preview with empty group uses Android Previews default group`() {
        val (result, compilation) = compileRetainingWithAndroidPreview(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.tooling.preview.Preview

                @Preview(name = "Chip")
                @Composable
                fun chipPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("\"Android Previews\""))
        assertTrue(content.contains("GeneratedAndroidPreviewGroup_Default"))
    }

    @Test
    fun `@Preview previews from different groups generate distinct synthetic group objects`() {
        val (result, compilation) = compileRetainingWithAndroidPreview(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.tooling.preview.Preview

                @Preview(name = "Button", group = "Buttons")
                @Composable
                fun buttonPreview() {}

                @Preview(name = "Card", group = "Cards")
                @Composable
                fun cardPreview() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("GeneratedAndroidPreviewGroup_Buttons"))
        assertTrue(content.contains("GeneratedAndroidPreviewGroup_Cards"))
        assertTrue(content.contains("\"Buttons\""))
        assertTrue(content.contains("\"Cards\""))
    }

    @Test
    fun `error when @Preview applied to non-Composable function`() {
        val result = compileWithAndroidPreview(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.ui.tooling.preview.Preview

                @Preview(name = "Bad")
                fun notComposable() {}
                """,
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("@Preview can only be applied to @Composable functions"))
    }

    @Test
    fun `@Preview and @ComposePreview are both emitted in the same registry`() {
        val (result, compilation) = compileRetainingWithAndroidPreview(
            SourceFile.kotlin(
                "Previews.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.tooling.preview.Preview
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "Compose Button", group = MyGroup::class)
                @Composable
                fun composeButton() {}

                @Preview(name = "Android Button", group = "Buttons")
                @Composable
                fun androidButton() {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", file)
        val content = file!!.readText()
        assertTrue(content.contains("\"com.example.composeButton\""))
        assertTrue(content.contains("\"com.example.androidButton\""))
        assertTrue(content.contains("group = MyGroup"))
        assertTrue(content.contains("group = GeneratedAndroidPreviewGroup_Buttons"))
    }

    // ── New param type tests ──────────────────────────────────────────────────

    @Test
    fun `param form emits PreviewDpField for Dp param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.unit.Dp
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Size", default = "16") size: Dp = Dp(16f),
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", file)
        val content = file!!.readText()
        assertTrue("Should use PreviewDpField", content.contains("PreviewDpField"))
        assertTrue("Default should be Dp(16.0f)", content.contains("Dp(16"))
    }

    @Test
    fun `param form emits PreviewTextUnitField for TextUnit param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.unit.TextUnit
                import androidx.compose.ui.unit.TextUnitType
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Font Size", default = "14") fontSize: TextUnit = TextUnit(14f, TextUnitType.Sp),
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val file = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", file)
        val content = file!!.readText()
        assertTrue("Should use PreviewTextUnitField", content.contains("PreviewTextUnitField"))
        assertTrue("Default should reference TextUnit", content.contains("TextUnit(14"))
    }

    @Test
    fun `param form emits PreviewNullableWrapper for nullable String param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Title") title: String? = null,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val form = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", form)
        val formContent = form!!.readText()
        assertTrue("Should use PreviewNullableWrapper", formContent.contains("PreviewNullableWrapper"))
        assertTrue("Should store isNull key", formContent.contains("title#isNull"))

        val registry = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull(registry)
        val regContent = registry!!.readText()
        assertTrue("Registry composable should handle null check", regContent.contains("title#isNull"))
    }

    @Test
    fun `param form emits PreviewDropdownField for enum param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }
                enum class ButtonVariant { Primary, Secondary, Destructive }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Variant", default = "Secondary")
                    variant: ButtonVariant = ButtonVariant.Secondary,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val form = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", form)
        val formContent = form!!.readText()
        assertTrue("Enum params should use PreviewDropdownField", formContent.contains("PreviewDropdownField"))
        assertTrue("Form should include enum default", formContent.contains("?: \"Secondary\""))
        assertTrue("Form should list Primary option", formContent.contains("\"Primary\""))
        assertTrue("Form should list Secondary option", formContent.contains("\"Secondary\""))
        assertTrue("Form should list Destructive option", formContent.contains("\"Destructive\""))
    }

    @Test
    fun `registry composable reads enum param from string state`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }
                enum class ButtonVariant { Primary, Secondary, Destructive }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Variant", default = "Secondary")
                    variant: ButtonVariant = ButtonVariant.Secondary,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val registry = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull("GeneratedPreviewRegistry.kt was not generated", registry)
        val regContent = registry!!.readText()
        assertTrue("Registry should read enum state as a String", regContent.contains("_state.get<String>(\"variant\")"))
        assertTrue("Registry should use the enum default name", regContent.contains("?: \"Secondary\""))
        assertTrue("Registry should convert state back to enum", regContent.contains("ButtonVariant.valueOf"))
    }

    @Test
    fun `param form emits data class section with sub-fields`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }
                data class ButtonConfig(val label: String, val enabled: Boolean)

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Config") config: ButtonConfig = ButtonConfig("OK", true),
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val form = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", form)
        val formContent = form!!.readText()
        assertTrue("Form should reference config.label sub-key", formContent.contains("config.label"))
        assertTrue("Form should reference config.enabled sub-key", formContent.contains("config.enabled"))

        val registry = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull(registry)
        val regContent = registry!!.readText()
        assertTrue("Registry should construct ButtonConfig", regContent.contains("ButtonConfig("))
    }

    @Test
    fun `param form emits sealed class variant dropdown and sub-fields`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }
                sealed class UiState {
                    object Loading : UiState()
                    data class Success(val message: String) : UiState()
                }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "State", default = "Loading") state: UiState = UiState.Loading,
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val form = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", form)
        val formContent = form!!.readText()
        assertTrue("Form should have variant dropdown", formContent.contains("state#variant"))
        assertTrue("Form should list Loading option", formContent.contains("\"Loading\""))
        assertTrue("Form should list Success option", formContent.contains("\"Success\""))

        val registry = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull(registry)
        val regContent = registry!!.readText()
        assertTrue("Registry should have when on variant", regContent.contains("state#variant"))
        assertTrue("Registry should construct UiState.Loading", regContent.contains("UiState.Loading"))
        assertTrue("Registry should construct UiState.Success", regContent.contains("UiState.Success"))
    }

    @Test
    fun `param form emits PreviewListField for List String param`() {
        val (result, compilation) = compileRetaining(
            SourceFile.kotlin(
                "Preview.kt",
                """
                package com.example
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.*

                object MyGroup : PreviewGroup { override val name = "G" }

                @ComposePreview(name = "T", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Tags", default = "alpha|beta") tags: List<String> = listOf("alpha", "beta"),
                ) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val form = findGeneratedFile(compilation, "myPreviewParamForm")
        assertNotNull("myPreviewParamForm.kt was not generated", form)
        val formContent = form!!.readText()
        assertTrue("Form should use PreviewListField", formContent.contains("PreviewListField"))
        assertTrue("Form should track item count", formContent.contains("tags#count"))

        val registry = findGeneratedFile(compilation, "GeneratedPreviewRegistry")
        assertNotNull(registry)
        val regContent = registry!!.readText()
        assertTrue("Registry should read count", regContent.contains("tags#count"))
        assertTrue("Registry defaults should contain count", regContent.contains("\"tags#count\" to 2"))
    }

    @Test
    fun `data class param is allowed in strict mode`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup
                import tech.lucam.composeum.annotation.PreviewParam

                object MyGroup : PreviewGroup { override val name = "Group" }
                data class Config(val text: String, val enabled: Boolean)

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Config") config: Config = Config("hello", true),
                ) {}
                """,
            ),
        )
        assertTrue("Data class should pass strict-mode validation",
            !result.messages.contains("error: @PreviewParam"))
    }

    @Test
    fun `sealed class param is allowed in strict mode`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup
                import tech.lucam.composeum.annotation.PreviewParam

                object MyGroup : PreviewGroup { override val name = "Group" }
                sealed class State { object Loading : State(); data class Done(val n: Int) : State() }

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "State", default = "Loading") state: State = State.Loading,
                ) {}
                """,
            ),
        )
        assertTrue("Sealed class should pass strict-mode validation",
            !result.messages.contains("error: @PreviewParam"))
    }

    @Test
    fun `List param is allowed in strict mode`() {
        val result = compile(
            SourceFile.kotlin(
                "Preview.kt",
                """
                import androidx.compose.runtime.Composable
                import tech.lucam.composeum.annotation.ComposePreview
                import tech.lucam.composeum.annotation.PreviewGroup
                import tech.lucam.composeum.annotation.PreviewParam

                object MyGroup : PreviewGroup { override val name = "Group" }

                @ComposePreview(name = "Test", group = MyGroup::class)
                @Composable
                fun myPreview(
                    @PreviewParam(label = "Items") items: List<String> = listOf("a"),
                ) {}
                """,
            ),
        )
        assertTrue("List<String> should pass strict-mode validation",
            !result.messages.contains("error: @PreviewParam"))
    }
}
