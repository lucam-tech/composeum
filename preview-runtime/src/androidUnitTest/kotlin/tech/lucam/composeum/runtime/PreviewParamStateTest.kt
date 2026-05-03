package tech.lucam.composeum.runtime

import kotlinx.collections.immutable.persistentMapOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewParamStateTest {

    @Test
    fun `put returns a new instance with updated value`() {
        val original = PreviewParamState(persistentMapOf("x" to "hello"))
        val updated = original.put("x", "world")
        assertNotSame(original, updated)
        assertEquals("hello", original.get<String>("x"))
        assertEquals("world", updated.get<String>("x"))
    }

    @Test
    fun `put adds new key without mutating original`() {
        val original = PreviewParamState(persistentMapOf())
        val updated = original.put("flag", true)
        assertNull(original.get<Boolean>("flag"))
        assertEquals(true, updated.get<Boolean>("flag"))
    }

    @Test
    fun `get returns null for missing key`() {
        val state = PreviewParamState(persistentMapOf())
        assertNull(state.get<String>("missing"))
    }

    @Test
    fun `get returns typed value`() {
        val state = PreviewParamState(persistentMapOf("count" to 42))
        assertEquals(42, state.get<Int>("count"))
    }

    @Test
    fun `typed keys provide typed put and get access`() {
        val countKey = previewParamKey<Int>("count")
        val state = PreviewParamState().put(countKey, 7)

        assertEquals(7, state[countKey])
    }

    @Test
    fun `toInitialState produces state matching defaults`() {
        val defaults = PreviewParamDefaults(mapOf("label" to "hello", "enabled" to true))
        val state = defaults.toInitialState()
        assertEquals("hello", state.get<String>("label"))
        assertEquals(true, state.get<Boolean>("enabled"))
    }

    @Test
    fun `toInitialState from empty defaults produces empty state`() {
        val state = PreviewParamDefaults(emptyMap()).toInitialState()
        assertEquals(0, state.values.size)
    }
}
