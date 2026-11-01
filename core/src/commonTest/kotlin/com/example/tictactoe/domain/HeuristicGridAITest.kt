package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.player.HeuristicGridAI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HeuristicGridAITest {
    @Test
    fun unbeatableBlocksImmediate4x4Threat() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable, maxDepth = 3)
        // Opponent O has cells 0, 1, 2 in 4x4. AI X must play 3 to block win.
        var board = GridBoard(size = 4, winLength = 4)
        board = board.place(0, Mark.O).place(1, Mark.O).place(2, Mark.O)
        val move = ai.chooseMove(board, Mark.X)
        assertEquals(3, move)
    }

    @Test
    fun unbeatableSeizesImmediate4x4Win() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable, maxDepth = 2)
        // AI X has cells 4, 5, 6 in 4x4. AI X must play 7 to win.
        var board = GridBoard(size = 4, winLength = 4)
        board = board.place(4, Mark.X).place(5, Mark.X).place(6, Mark.X)
        val move = ai.chooseMove(board, Mark.X)
        assertEquals(7, move)
    }

    @Test
    fun decisionTimeIsUnder150msOn5x5() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable, maxDepth = 3)
        val board = GridBoard(size = 5, winLength = 4)
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val move = ai.chooseMove(board, Mark.X)
        val elapsed = start.elapsedNow().inWholeMilliseconds
        assertTrue(move in board.emptyCells)
        assertTrue(elapsed < 150, "AI took ${elapsed}ms, should be <150ms")
    }

    @Test
    fun decisionTimeIsUnder150msOn5x5WithExistingMoves() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable, maxDepth = 3)
        var board = GridBoard(size = 5, winLength = 4)
        board = board.place(12, Mark.O).place(6, Mark.X).place(7, Mark.O)
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val move = ai.chooseMove(board, Mark.X)
        val elapsed = start.elapsedNow().inWholeMilliseconds
        assertTrue(move in board.emptyCells)
        assertTrue(elapsed < 150, "AI took ${elapsed}ms, should be <150ms")
    }

    @Test
    fun defaultMaxDepthsMatchDifficulty() {
        assertEquals(1, HeuristicGridAI(Difficulty.Easy).maxDepth)
        assertEquals(2, HeuristicGridAI(Difficulty.Medium).maxDepth)
        assertEquals(3, HeuristicGridAI(Difficulty.Unbeatable).maxDepth)
    }

    @Test
    fun easyModeCanMakeBlunderMove() {
        // Random seeded to return a non-zero index on nextInt(4)
        val seededRandom = kotlin.random.Random(42)
        val ai = HeuristicGridAI(difficulty = Difficulty.Easy, random = seededRandom)
        var board = GridBoard(size = 4, winLength = 4)
        board = board.place(0, Mark.O).place(1, Mark.O).place(2, Mark.O)
        val move = ai.chooseMove(board, Mark.X)
        assertTrue(move in board.emptyCells)
    }

    @Test
    fun emptyBoardPicksCenterMove() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable)
        val board4 = GridBoard(size = 4, winLength = 4)
        val move4 = ai.chooseMove(board4, Mark.X)
        assertEquals(5, move4)

        val board5 = GridBoard(size = 5, winLength = 4)
        val move5 = ai.chooseMove(board5, Mark.X)
        assertEquals(12, move5)
    }

    @Test
    fun fullBoardThrowsIllegalStateException() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable)
        val fullCells = List(9) { if (it % 2 == 0) Mark.X else Mark.O }
        val fullBoard = GridBoard(size = 3, winLength = 3, cells = fullCells)
        assertFailsWith<IllegalStateException> {
            ai.chooseMove(fullBoard, Mark.X)
        }
    }
}
