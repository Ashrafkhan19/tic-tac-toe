package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Mode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchTest {
    private val twoPlayer = Match(settings = MatchSettings(mode = Mode.TwoPlayer))
    private fun Match.playAll(vararg moves: Int) = moves.fold(this) { m, i -> m.play(i) }

    @Test
    fun winIsScoredOnce() {
        val m = twoPlayer.playAll(0, 3, 1, 4, 2, 8)
        assertEquals(1, m.scoreboard.xWins)
        assertEquals(0, m.scoreboard.oWins)
    }

    @Test
    fun drawIsScored() {
        assertEquals(1, twoPlayer.playAll(0, 1, 2, 4, 3, 5, 7, 6, 8).scoreboard.draws)
    }

    @Test
    fun computerTurnOnlyInVsComputerAfterHumanMove() {
        val vsCpu = Match()
        assertFalse(vsCpu.isComputerTurn)
        assertTrue(vsCpu.play(0).isComputerTurn)
        assertFalse(twoPlayer.play(0).isComputerTurn)
    }

    @Test
    fun nextRoundAlternatesOpenerAndKeepsScores() {
        val won = twoPlayer.playAll(0, 3, 1, 4, 2)
        val next = won.nextRound()
        assertEquals(Mark.O, next.game.turn)
        assertEquals(1, next.scoreboard.xWins)
        assertEquals(9, next.game.board.emptyCells.size)
        assertEquals(Mark.X, next.nextRound().game.turn)
    }

    @Test
    fun newSettingsClearScores() {
        val won = twoPlayer.playAll(0, 3, 1, 4, 2)
        val changed = won.withSettings(won.settings.copy(difficulty = Difficulty.Easy))
        assertEquals(0, changed.scoreboard.xWins)
        assertEquals(Mode.TwoPlayer, changed.settings.mode)
        assertEquals(Difficulty.Easy, changed.settings.difficulty)
    }
}
