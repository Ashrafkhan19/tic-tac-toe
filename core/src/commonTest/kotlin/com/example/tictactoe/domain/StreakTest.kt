package com.example.tictactoe.domain

import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.StreakState
import com.example.tictactoe.domain.progress.XpRules
import com.example.tictactoe.domain.progress.completed
import com.example.tictactoe.domain.progress.displayed
import com.example.tictactoe.domain.progress.isAtRisk
import com.example.tictactoe.domain.usecase.CompleteDailyChallenge
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakTest {
    private val d1 = LocalDate(2026, 11, 1)
    private val d2 = LocalDate(2026, 11, 2)
    private val d3 = LocalDate(2026, 11, 3)
    private val d4 = LocalDate(2026, 11, 4)

    @Test
    fun firstCompletion() {
        val s = StreakState().completed(d1)
        assertEquals(StreakState(1, 1, 0, d1), s)
    }

    @Test
    fun sameDayTwiceUnchanged() {
        val s = StreakState().completed(d1)
        assertEquals(s, s.completed(d1))
    }

    @Test
    fun nextDayIncrements() {
        assertEquals(2, StreakState().completed(d1).completed(d2).current)
    }

    @Test
    fun gapTwoWithFreezeBridges() {
        val s = StreakState(current = 3, best = 3, freezes = 1, lastCompleted = d1).completed(d3)
        assertEquals(4, s.current)
        assertEquals(0, s.freezes)
        assertEquals(d3, s.lastCompleted)
    }

    @Test
    fun gapTwoWithoutFreezeResets() {
        val s = StreakState(current = 3, best = 3, freezes = 0, lastCompleted = d1).completed(d3)
        assertEquals(1, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun gapThreeBreaksButKeepsFreeze() {
        val s = StreakState(current = 3, best = 3, freezes = 1, lastCompleted = d1).completed(d4)
        assertEquals(1, s.current)
        assertEquals(1, s.freezes)
    }

    @Test
    fun earlierDateUnchanged() {
        val s = StreakState(current = 3, best = 3, freezes = 0, lastCompleted = d3)
        assertEquals(s, s.completed(d1))
    }

    @Test
    fun seventhDayEarnsFreeze() {
        var s = StreakState()
        for (i in 0 until 7) s = s.completed(LocalDate.fromEpochDays(d1.toEpochDays() + i))
        assertEquals(7, s.current)
        assertEquals(1, s.freezes)
    }

    @Test
    fun freezeCappedAtOne() {
        var s = StreakState()
        for (i in 0 until 14) s = s.completed(LocalDate.fromEpochDays(d1.toEpochDays() + i))
        assertEquals(14, s.current)
        assertEquals(1, s.freezes)
    }

    @Test
    fun bestSurvivesReset() {
        val s = StreakState(current = 5, best = 5, freezes = 0, lastCompleted = d1).completed(d4)
        assertEquals(1, s.current)
        assertEquals(5, s.best)
    }

    @Test
    fun displayedAfterGaps() {
        val noFreeze = StreakState(current = 4, best = 4, freezes = 0, lastCompleted = d1)
        assertEquals(4, noFreeze.displayed(d1))
        assertEquals(4, noFreeze.displayed(d2))
        assertEquals(0, noFreeze.displayed(d3))
        val frozen = noFreeze.copy(freezes = 1)
        assertEquals(4, frozen.displayed(d3))
        assertEquals(0, frozen.displayed(d4))
        assertEquals(4, frozen.displayed(LocalDate(2026, 10, 30)))
        assertEquals(0, StreakState().displayed(d1))
    }

    @Test
    fun atRisk() {
        val s = StreakState().completed(d1)
        assertFalse(s.isAtRisk(d1))
        assertTrue(s.isAtRisk(d2))
        assertFalse(StreakState().isAtRisk(d1))
    }

    @Test
    fun atRiskOnlyForALiveStreakOneOrTwoDaysOld() {
        val s = StreakState().completed(d2)
        assertFalse(s.isAtRisk(d1), "clock moved backwards: lastCompleted is in the future")
        assertTrue(s.isAtRisk(d3))
        assertFalse(s.isAtRisk(d4), "gap of two without a freeze has lapsed")
        val frozen = s.copy(freezes = 1)
        assertTrue(frozen.isAtRisk(d4), "gap of two bridged by a freeze")
        assertFalse(frozen.isAtRisk(LocalDate(2026, 11, 5)))
    }

    @Test
    fun completeDailyChallengeIsIdempotent() {
        val use = CompleteDailyChallenge()
        val once = use(PlayerProgress(), d1)
        assertEquals(XpRules.CHALLENGE_BONUS, once.xp)
        assertEquals(1, once.streak.current)
        assertEquals(once, use(once, d1))
        assertEquals(once, use(once, LocalDate(2026, 10, 30)))
        assertEquals(2 * XpRules.CHALLENGE_BONUS, use(once, d2).xp)
    }
}
