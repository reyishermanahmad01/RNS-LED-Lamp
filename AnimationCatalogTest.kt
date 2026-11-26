package com.rns.controller

import com.rns.controller.domain.catalog.AnimationCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationCatalogTest {
    @Test
    fun catalogContainsExactly100SequentialSlots() {
        assertEquals(100, AnimationCatalog.slots.size)
        AnimationCatalog.slots.forEachIndexed { index, slot ->
            assertEquals(index + 1, slot.id)
        }
    }

    @Test
    fun catalogContains12FullWhiteStrobeVariations() {
        val whiteStrobes = AnimationCatalog.slots.filter { it.isWhiteStrobe }
        assertEquals(12, whiteStrobes.size)
        assertTrue(whiteStrobes.all { it.category == "Strobo Full White" })
    }

    @Test
    fun dmxCatalogContainsExactly210SequentialModes() {
        assertEquals(210, AnimationCatalog.dmxModes.size)
        AnimationCatalog.dmxModes.forEachIndexed { index, slot ->
            assertEquals(index + 1, slot.id)
        }
        assertTrue(AnimationCatalog.dmxModes.any { it.group == "Strobo Full White DMX" })
    }
}
