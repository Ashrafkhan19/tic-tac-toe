package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GridBoardTest {
    @Test
    fun grid4x4DetectsHorizontalWin() {
        var board = GridBoard(size = 4, winLength = 4)
        listOf(0, 1, 2, 3).forEach { board = board.place(it, Mark.X) }
        val outcome = board.outcome()
        assertTrue(outcome is Outcome.Won)
        assertEquals(Mark.X, outcome.winner)
        assertEquals(listOf(0, 1, 2, 3), outcome.line)
    }

    @Test
    fun grid4x4DetectsVerticalWin() {
        var board = GridBoard(size = 4, winLength = 4)
        listOf(1, 5, 9, 13).forEach { board = board.place(it, Mark.O) }
        val outcome = board.outcome()
        assertTrue(outcome is Outcome.Won)
        assertEquals(Mark.O, outcome.winner)
        assertEquals(listOf(1, 5, 9, 13), outcome.line)
    }

    @Test
    fun grid5x5Detects4InARowDiagonalWinWithoutWrap() {
        var board = GridBoard(size = 5, winLength = 4)
        // Diagonal: (0,0)=0, (1,1)=6, (2,2)=12, (3,3)=18
        listOf(0, 6, 12, 18).forEach { board = board.place(it, Mark.O) }
        val outcome = board.outcome()
        assertTrue(outcome is Outcome.Won)
        assertEquals(Mark.O, outcome.winner)
        assertEquals(listOf(0, 6, 12, 18), outcome.line)
    }

    @Test
    fun grid4x4DetectsAntiDiagonalWin() {
        var board = GridBoard(size = 4, winLength = 4)
        // Anti-diagonal: (0,3)=3, (1,2)=6, (2,1)=9, (3,0)=12
        listOf(3, 6, 9, 12).forEach { board = board.place(it, Mark.X) }
        val outcome = board.outcome()
        assertTrue(outcome is Outcome.Won)
        assertEquals(Mark.X, outcome.winner)
        assertEquals(listOf(3, 6, 9, 12), outcome.line)
    }

    @Test
    fun grid4x4DetectsDrawWhenBoardFullWithoutWinner() {
        val pattern = listOf(
            Mark.X, Mark.O, Mark.X, Mark.O,
            Mark.X, Mark.O, Mark.X, Mark.O,
            Mark.O, Mark.X, Mark.O, Mark.X,
            Mark.O, Mark.X, Mark.O, Mark.X,
        )
        val board = GridBoard(size = 4, winLength = 4, cells = pattern)
        assertEquals(Outcome.Draw, board.outcome())
    }

    @Test
    fun emptyBoardIsInProgress() {
        val board = GridBoard(size = 4, winLength = 4)
        assertEquals(Outcome.InProgress, board.outcome())
        assertEquals(16, board.emptyCells.size)
    }

    @Test
    fun cannotPlaceOnOccupiedOrOutOfBoundsCell() {
        val board = GridBoard(size = 4, winLength = 4).place(0, Mark.X)
        assertFailsWith<IllegalArgumentException> {
            board.place(0, Mark.O)
        }
        assertFailsWith<IllegalArgumentException> {
            board.place(-1, Mark.X)
        }
        assertFailsWith<IllegalArgumentException> {
            board.place(16, Mark.X)
        }
    }

    @Test
    fun boardValidationsEnforced() {
        assertFailsWith<IllegalArgumentException> {
            GridBoard(size = 2, winLength = 2)
        }
        assertFailsWith<IllegalArgumentException> {
            GridBoard(size = 4, winLength = 2)
        }
        assertFailsWith<IllegalArgumentException> {
            GridBoard(size = 4, winLength = 5)
        }
        assertFailsWith<IllegalArgumentException> {
            GridBoard(size = 3, winLength = 3, cells = List(8) { null })
        }
    }

    @Test
    fun gameVariantValuesAndMoves() {
        assertEquals("Classic (3×3)", GameVariant.CLASSIC_3X3.displayName)
        assertEquals("Grid (4×4)", GameVariant.GRID_4X4.displayName)
        assertEquals("Grid (5×5)", GameVariant.GRID_5X5.displayName)
        assertEquals("Ultimate", GameVariant.ULTIMATE.displayName)

        val gridMove = GameMove.Grid(5)
        assertEquals(5, gridMove.index)

        val ultMove = GameMove.Ultimate(1, 2)
        assertEquals(1, ultMove.macroIndex)
        assertEquals(2, ultMove.microIndex)
    }
}
