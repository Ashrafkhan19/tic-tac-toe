package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.player.ComputerPlayer
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChooseComputerMove
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.ResetScores
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class UseCasesTest {
    @Test
    fun humanCannotMoveDuringComputerTurn() {
        val afterHuman = PlayHumanMove()(Match(), 0)
        assertSame(afterHuman, PlayHumanMove()(afterHuman, 1))
    }

    @Test
    fun computerMoveOnlyAppliesOnComputerTurn() {
        val fresh = Match()
        assertSame(fresh, ApplyComputerMove()(fresh, 4))
        val applied = ApplyComputerMove()(PlayHumanMove()(fresh, 0), 4)
        assertEquals(2, 9 - applied.game.board.emptyCells.size)
    }

    @Test
    fun chooseComputerMoveUsesPlayerForSelectedDifficulty() {
        val picked = mutableListOf<Difficulty>()
        val use = ChooseComputerMove { difficulty ->
            picked += difficulty
            ComputerPlayer { 7 }
        }
        val match = Match(settings = MatchSettings(Mode.VsComputer, Difficulty.Easy))
        assertEquals(7, use(match))
        assertEquals(listOf(Difficulty.Easy), picked)
    }

    @Test
    fun resetScoresKeepsSettings() {
        val two = Match(settings = MatchSettings(Mode.TwoPlayer, Difficulty.Easy))
        val won = listOf(0, 3, 1, 4, 2).fold(two) { m, i -> m.play(i) }
        assertEquals(Match(settings = two.settings), ResetScores()(won))
    }
}
