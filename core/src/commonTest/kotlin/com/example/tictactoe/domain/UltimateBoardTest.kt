package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UltimateBoardTest {

    @Test
    fun initialBoardAllowsAnySubBoardMove() {
        val board = UltimateBoard()
        assertEquals(81, board.legalMoves().size)
        assertNull(board.activeMacroIndex)
        assertEquals(Outcome.InProgress, board.outcome())
    }

    @Test
    fun moveDirectsOpponentToTargetSubBoard() {
        val board = UltimateBoard().place(GameMove.Ultimate(macroIndex = 4, microIndex = 2), Mark.X)
        assertEquals(2, board.activeMacroIndex)
        assertTrue(board.legalMoves().all { it.macroIndex == 2 })
        assertEquals(9, board.legalMoves().size)
    }

    @Test
    fun winningSubBoardClaimsMacroCellAndAllowsFreeChoiceIfTargetIsFinished() {
        var board = UltimateBoard()
        // Win sub-board 0 for X with row (0, 1, 2)
        board = board.place(GameMove.Ultimate(0, 1), Mark.X) // next 1
        board = board.place(GameMove.Ultimate(1, 0), Mark.O) // next 0
        board = board.place(GameMove.Ultimate(0, 2), Mark.X) // next 2
        board = board.place(GameMove.Ultimate(2, 0), Mark.O) // next 0
        board = board.place(GameMove.Ultimate(0, 0), Mark.X) // X wins sub-board 0 with (0,1,2)! Target is 0, which is finished

        val subOutcome = board.subBoards[0].outcome()
        assertTrue(subOutcome is Outcome.Won)
        assertEquals(Mark.X, subOutcome.winner)
        assertEquals(Mark.X, board.macroBoard.cells[0])
        assertNull(board.activeMacroIndex)
    }

    @Test
    fun directingToAlreadyWonSubBoardGrantsFreeChoice() {
        var board = UltimateBoard()
        // Win sub-board 0 for X
        board = board.place(GameMove.Ultimate(0, 1), Mark.X) // next 1
        board = board.place(GameMove.Ultimate(1, 0), Mark.O) // next 0
        board = board.place(GameMove.Ultimate(0, 2), Mark.X) // next 2
        board = board.place(GameMove.Ultimate(2, 0), Mark.O) // next 0
        board = board.place(GameMove.Ultimate(0, 0), Mark.X) // X wins sub-board 0; free choice

        // Player O uses free choice to play in sub-board 4, sending X to sub-board 0 (microIndex = 0)
        board = board.place(GameMove.Ultimate(4, 0), Mark.O)
        // Sub-board 0 is already won, so player X must have free choice (activeMacroIndex = null)
        assertNull(board.activeMacroIndex)

        // Legal moves should be across all remaining in-progress sub-boards (1..8)
        val legalMoves = board.legalMoves()
        assertTrue(legalMoves.none { it.macroIndex == 0 })
        assertTrue(legalMoves.any { it.macroIndex == 1 })
        assertTrue(legalMoves.any { it.macroIndex == 2 })
        assertTrue(legalMoves.any { it.macroIndex == 8 })
    }

    @Test
    fun directingToDrawnSubBoardGrantsFreeChoice() {
        // Construct a sub-board 3 that is a draw:
        // X O X
        // X X O
        // O X O
        val drawnSubBoard = GridBoard(3, 3, listOf(
            Mark.X, Mark.O, Mark.X,
            Mark.X, Mark.X, Mark.O,
            Mark.O, Mark.X, Mark.O,
        ))
        assertEquals(Outcome.Draw, drawnSubBoard.outcome())

        val subBoards = List(9) { if (it == 3) drawnSubBoard else GridBoard(3, 3) }
        val board = UltimateBoard(subBoards = subBoards)

        // Make a move that targets sub-board 3
        val nextBoard = board.place(GameMove.Ultimate(macroIndex = 0, microIndex = 3), Mark.X)

        // Since sub-board 3 is drawn, activeMacroIndex should be null (free choice)
        assertNull(nextBoard.activeMacroIndex)
        assertTrue(nextBoard.legalMoves().none { it.macroIndex == 3 })
    }

    @Test
    fun macroWinImmediatelyResolvesOutcomeWonAndDisallowsFurtherMoves() {
        // Pre-configure subBoards where subBoards 0 and 1 are won by X, and subBoard 2 is one move away from being won by X
        // Sub-board 0 won by X (cells 0, 1, 2)
        val wonSubBoard0 = GridBoard(3, 3).place(0, Mark.X).place(1, Mark.X).place(2, Mark.X)
        // Sub-board 1 won by X (cells 0, 1, 2)
        val wonSubBoard1 = GridBoard(3, 3).place(0, Mark.X).place(1, Mark.X).place(2, Mark.X)
        // Sub-board 2 has X at 0 and 1, empty at 2
        val nearWonSubBoard2 = GridBoard(3, 3).place(0, Mark.X).place(1, Mark.X)

        val subBoards = List(9) { i ->
            when (i) {
                0 -> wonSubBoard0
                1 -> wonSubBoard1
                2 -> nearWonSubBoard2
                else -> GridBoard(3, 3)
            }
        }
        val macroBoard = GridBoard(3, 3).place(0, Mark.X).place(1, Mark.X)
        val board = UltimateBoard(
            subBoards = subBoards,
            macroBoard = macroBoard,
            activeMacroIndex = 2
        )

        // Player X completes sub-board 2 at microIndex 2, completing row 0-1-2 on the macro board
        val winningBoard = board.place(GameMove.Ultimate(2, 2), Mark.X)

        val outcome = winningBoard.outcome()
        assertTrue(outcome is Outcome.Won)
        assertEquals(Mark.X, outcome.winner)
        assertEquals(listOf(0, 1, 2), outcome.line)
        assertTrue(winningBoard.legalMoves().isEmpty())

        assertFailsWith<IllegalArgumentException> {
            winningBoard.place(GameMove.Ultimate(3, 0), Mark.O)
        }
    }

    @Test
    fun macroDrawResolvesOutcomeDraw() {
        // Macro board full without 3-in-a-row is a draw
        // X O X
        // X X O
        // O X O
        val macroMarks = listOf(
            Mark.X, Mark.O, Mark.X,
            Mark.X, Mark.X, Mark.O,
            Mark.O, Mark.X, Mark.O,
        )
        var macroBoard = GridBoard(3, 3)
        macroMarks.forEachIndexed { idx, mark ->
            macroBoard = macroBoard.place(idx, mark)
        }
        assertEquals(Outcome.Draw, macroBoard.outcome())

        val board = UltimateBoard(macroBoard = macroBoard)
        assertEquals(Outcome.Draw, board.outcome())
        assertTrue(board.legalMoves().isEmpty())
    }

    @Test
    fun invalidMovesThrowIllegalArgumentException() {
        val board = UltimateBoard().place(GameMove.Ultimate(0, 0), Mark.X) // next active = 0
        // Move outside active sub-board
        assertFailsWith<IllegalArgumentException> {
            board.place(GameMove.Ultimate(1, 0), Mark.O)
        }
        // Move into already occupied cell
        assertFailsWith<IllegalArgumentException> {
            board.place(GameMove.Ultimate(0, 0), Mark.O)
        }
    }
}
