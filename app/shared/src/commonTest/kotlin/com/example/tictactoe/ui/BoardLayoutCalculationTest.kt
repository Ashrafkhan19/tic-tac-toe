package com.example.tictactoe.ui

import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import com.example.tictactoe.ui.components.BoardLayoutCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardLayoutCalculationTest {
    @Test
    fun gridCellSizeScalesCorrectlyForDifferentSizes() {
        val containerSize = 360f
        val gap = 8f
        // 4x4 has 4 cells and 3 gaps
        val cellSize4 = (containerSize - (gap * 3)) / 4
        // 5x5 has 5 cells and 4 gaps
        val cellSize5 = (containerSize - (gap * 4)) / 5

        assertEquals(84f, cellSize4)
        assertEquals(65.6f, cellSize5)
    }

    @Test
    fun boardLayoutCalculatorComputesCellDimensions() {
        val containerSize = 360f
        val gap = 8f
        val cellSize4 = BoardLayoutCalculator.calculateCellSize(containerSize, gap, 4)
        val cellSize5 = BoardLayoutCalculator.calculateCellSize(containerSize, gap, 5)

        assertEquals(84f, cellSize4)
        assertEquals(65.6f, cellSize5)
    }

    @Test
    fun boardLayoutCalculatorComputesCellCenterCoordinates() {
        val gap = 6f
        val cellSize = 80f
        // index 0 -> row 0, col 0 -> center at (40, 40)
        val center0 = BoardLayoutCalculator.cellCenter(0, 4, cellSize, gap)
        assertEquals(40f to 40f, center0)

        // index 5 in 4x4 -> row 1, col 1 -> (1*(80+6)+40, 1*(80+6)+40) = (126, 126)
        val center5 = BoardLayoutCalculator.cellCenter(5, 4, cellSize, gap)
        assertEquals(126f to 126f, center5)
    }

    @Test
    fun boardLayoutCalculator3x3CellCenterAndDimensions() {
        val containerSize = 300f
        val gap = 10f
        val cellSize3 = BoardLayoutCalculator.calculateCellSize(containerSize, gap, 3)
        // (300 - 20) / 3 = 93.333336
        assertEquals((300f - 20f) / 3f, cellSize3)

        // index 8 in 3x3 -> row 2, col 2
        val center8 = BoardLayoutCalculator.cellCenter(8, 3, 90f, 10f)
        // 2 * (90 + 10) + 45 = 245
        assertEquals(245f to 245f, center8)
    }

    @Test
    fun ultimateBoardTargetingResolution() {
        val initialBoard = UltimateBoard()
        // Fresh board: activeMacroIndex is null -> all 9 sub-boards are targets
        for (macro in 0 until 9) {
            val isTarget = initialBoard.activeMacroIndex == null || initialBoard.activeMacroIndex == macro
            val isComplete = initialBoard.subBoards[macro].outcome() !is Outcome.InProgress
            assertTrue(isTarget && !isComplete)
        }

        // Place move in macro 0, micro 4 -> next target must be macro 4
        val afterMove = initialBoard.place(GameMove.Ultimate(0, 4), Mark.X)
        assertEquals(4, afterMove.activeMacroIndex)

        for (macro in 0 until 9) {
            val isTarget = afterMove.activeMacroIndex == null || afterMove.activeMacroIndex == macro
            val isComplete = afterMove.subBoards[macro].outcome() !is Outcome.InProgress
            if (macro == 4) {
                assertTrue(isTarget && !isComplete)
            } else {
                assertFalse(isTarget)
            }
        }
    }
}
