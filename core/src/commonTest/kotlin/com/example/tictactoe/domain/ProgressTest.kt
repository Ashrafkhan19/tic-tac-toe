package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.progress.GameResult
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.XpRules
import com.example.tictactoe.domain.usecase.RecordGameResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressTest {
    @Test
    fun xpTable() {
        val d = Difficulty.entries
        assertEquals(listOf(10, 20, 40), d.map { XpRules.forGame(it, GameResult.Win) })
        assertEquals(listOf(3, 6, 15), d.map { XpRules.forGame(it, GameResult.Draw) })
        assertEquals(listOf(1, 1, 1), d.map { XpRules.forGame(it, GameResult.Loss) })
    }

    @Test
    fun levelCurve() {
        assertEquals(1, XpRules.levelFor(0))
        assertEquals(0, XpRules.xpToReach(1))
        for (n in 2..30) {
            assertEquals(n, XpRules.levelFor(XpRules.xpToReach(n)))
            assertEquals(n - 1, XpRules.levelFor(XpRules.xpToReach(n) - 1))
            assertTrue(XpRules.xpToReach(n) > XpRules.xpToReach(n - 1))
        }
    }

    private val record = RecordGameResult()
    private val medium = MatchSettings(Mode.VsComputer, Difficulty.Medium)

    @Test
    fun humanWin() {
        val p = record(PlayerProgress(), medium, Outcome.Won(Mark.X, listOf(0, 1, 2)))
        assertEquals(1, p.stats.getValue(Difficulty.Medium).wins)
        assertEquals(20, p.xp)
    }

    @Test
    fun humanLoss() {
        val p = record(PlayerProgress(), medium, Outcome.Won(Mark.O, listOf(0, 1, 2)))
        assertEquals(1, p.stats.getValue(Difficulty.Medium).losses)
        assertEquals(1, p.xp)
    }

    @Test
    fun unbeatableDraw() {
        val p = record(PlayerProgress(), MatchSettings(Mode.VsComputer, Difficulty.Unbeatable), Outcome.Draw)
        assertEquals(1, p.stats.getValue(Difficulty.Unbeatable).draws)
        assertEquals(15, p.xp)
    }

    @Test
    fun inProgressAndTwoPlayerUnchanged() {
        val start = PlayerProgress(xp = 5)
        assertEquals(start, record(start, medium, Outcome.InProgress))
        val two = MatchSettings(Mode.TwoPlayer, Difficulty.Medium)
        assertEquals(start, record(start, two, Outcome.Won(Mark.X, listOf(0, 1, 2))))
        assertEquals(start, record(start, two, Outcome.Draw))
    }
}
