package com.example.tictactoe.ui

import com.example.tictactoe.domain.model.GameVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class VariantPickerTest {
    @Test
    fun allVariantsHaveDisplayLabels() {
        val labels = GameVariant.entries.map { it.displayName }
        assertEquals(4, labels.size)
        assertEquals("Classic (3×3)", labels[0])
        assertEquals("Grid (4×4)", labels[1])
        assertEquals("Grid (5×5)", labels[2])
        assertEquals("Ultimate", labels[3])
    }

    @Test
    fun variantsAreOrderedAsExpected() {
        val variants = GameVariant.entries
        assertEquals(GameVariant.CLASSIC_3X3, variants[0])
        assertEquals(GameVariant.GRID_4X4, variants[1])
        assertEquals(GameVariant.GRID_5X5, variants[2])
        assertEquals(GameVariant.ULTIMATE, variants[3])
    }

    @Test
    fun eachVariantDisplayNameIsNonEmptyAndUnique() {
        val labels = GameVariant.entries.map { it.displayName }
        assertEquals(labels.size, labels.toSet().size)
        labels.forEach { label ->
            assertEquals(false, label.isBlank())
        }
    }
}
