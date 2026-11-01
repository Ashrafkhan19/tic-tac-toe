package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import com.example.tictactoe.domain.player.UltimateAI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UltimateAITest {
    @Test
    fun aiCompletesImmediateSubBoardWin() {
        val ai = UltimateAI(Difficulty.Unbeatable, maxDepth = 2)
        var board = UltimateBoard(activeMacroIndex = 0)
        // Sub-board 0 has X at 0 and 1. AI X playing in sub-board 0 must pick 2 to claim it!
        board = board.place(GameMove.Ultimate(0, 0), Mark.X)
        board = board.place(GameMove.Ultimate(0, 3), Mark.O)
        board = board.place(GameMove.Ultimate(3, 0), Mark.O)
        board = board.place(GameMove.Ultimate(0, 1), Mark.X)
        // Set active board to 0
        board = board.copy(activeMacroIndex = 0)
        val move = ai.chooseMove(board, Mark.X)
        assertEquals(0, move.macroIndex)
        assertEquals(2, move.microIndex)
    }

    @Test
    fun decisionTimeIsUnder150msOnUltimateBoard() {
        val ai = UltimateAI(Difficulty.Unbeatable, maxDepth = 2)
        val board = UltimateBoard()
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val move = ai.chooseMove(board, Mark.X)
        val elapsed = start.elapsedNow().inWholeMilliseconds
        assertTrue(move in board.legalMoves())
        assertTrue(elapsed < 150, "Ultimate AI took ${elapsed}ms, should be <150ms")
    }

    @Test
    fun aiWinsMacroBoardWhenPossible() {
        val ai = UltimateAI(Difficulty.Unbeatable, maxDepth = 2)
        // Set up macro board where sub-board 0 and 1 are already won by X.
        // Sub-board 2 has X at micro 0 and 1.
        // AI playing X with activeMacroIndex = 2 should play (2, 2) to win sub-board 2 and the macro game.
        var board = UltimateBoard()
        // Win sub-board 0 for X: (0,0), (0,1), (0,2)
        // Play moves to win sub-board 0 without violating UltimateBoard rules:
        // We can construct board with macroBoard and subBoards directly
        val winSub0 = board.subBoards[0]
            .place(0, Mark.X)
            .place(1, Mark.X)
            .place(2, Mark.X)
        val winSub1 = board.subBoards[1]
            .place(0, Mark.X)
            .place(1, Mark.X)
            .place(2, Mark.X)
        val partialSub2 = board.subBoards[2]
            .place(0, Mark.X)
            .place(1, Mark.X)
        val updatedMacro = board.macroBoard
            .place(0, Mark.X)
            .place(1, Mark.X)
        val subBoards = board.subBoards.toMutableList().apply {
            this[0] = winSub0
            this[1] = winSub1
            this[2] = partialSub2
        }
        board = board.copy(
            subBoards = subBoards,
            macroBoard = updatedMacro,
            activeMacroIndex = 2
        )

        val move = ai.chooseMove(board, Mark.X)
        assertEquals(2, move.macroIndex)
        assertEquals(2, move.microIndex)
        val nextBoard = board.place(move, Mark.X)
        assertTrue(nextBoard.outcome() is Outcome.Won)
        assertEquals(Mark.X, (nextBoard.outcome() as Outcome.Won).winner)
    }

    @Test
    fun aiThrowsWhenNoLegalMoves() {
        val ai = UltimateAI(Difficulty.Unbeatable, maxDepth = 2)
        // Fill all sub-boards so outcome is not InProgress
        var fullSub = boardWithSize3()
        for (i in 0 until 9) {
            fullSub = fullSub.place(i, if (i % 2 == 0) Mark.X else Mark.O)
        }
        val terminalBoard = UltimateBoard(
            subBoards = List(9) { fullSub },
            macroBoard = fullSub,
            activeMacroIndex = null
        )
        assertFailsWith<IllegalStateException> {
            ai.chooseMove(terminalBoard, Mark.X)
        }
    }

    @Test
    fun aiEasyPicksLegalMove() {
        val ai = UltimateAI(Difficulty.Easy)
        val board = UltimateBoard(activeMacroIndex = 4)
        val move = ai.chooseMove(board, Mark.O)
        assertTrue(move in board.legalMoves())
        assertEquals(4, move.macroIndex)
    }

    private fun boardWithSize3() = com.example.tictactoe.domain.model.GridBoard(size = 3, winLength = 3)
}
