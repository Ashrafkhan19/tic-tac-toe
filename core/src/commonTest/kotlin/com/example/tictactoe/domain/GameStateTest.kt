package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class GameStateTest {
    private fun play(vararg moves: Int) = moves.fold(GameState()) { s, m -> s.play(m) }

    @Test
    fun xGoesFirstAndTurnsAlternate() {
        val s = play(0)
        assertEquals(Mark.X, s.board.cells[0])
        assertEquals(Mark.O, s.turn)
    }

    @Test
    fun occupiedCellIsIgnored() {
        val s = play(0)
        assertSame(s, s.play(0))
    }

    @Test
    fun detectsRowWinWithLine() {
        assertEquals(Outcome.Won(Mark.X, listOf(0, 1, 2)), play(0, 3, 1, 4, 2).outcome)
    }

    @Test
    fun detectsDiagonalWinForO() {
        assertEquals(Outcome.Won(Mark.O, listOf(0, 4, 8)), play(1, 0, 2, 4, 5, 8).outcome)
    }

    @Test
    fun detectsDraw() {
        assertEquals(Outcome.Draw, play(0, 1, 2, 4, 3, 5, 7, 6, 8).outcome)
    }

    @Test
    fun noMovesAfterGameOver() {
        val s = play(0, 3, 1, 4, 2)
        assertSame(s, s.play(8))
    }
}
