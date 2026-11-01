package com.example.tictactoe.domain.focus

import com.example.tictactoe.domain.progress.PlayerProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusDomainTest {
    @Test
    fun dailyCircuitCompletion_requiresAllThreeCoreSteps() {
        val initial = DailyCircuitState(dateIso = "2026-11-01")
        assertFalse(initial.isComplete)

        val withWarmup = initial.copy(urgeWarmupCompleted = true)
        assertFalse(withWarmup.isComplete)

        val withNeuro = withWarmup.copy(neuroDrillCompleted = true)
        assertFalse(withNeuro.isComplete)

        val complete = withNeuro.copy(deepActivityCompleted = true)
        assertTrue(complete.isComplete)
    }

    @Test
    fun focusXpRules_calculatesAppropriateRewards() {
        assertEquals(20, FocusXpRules.xpForUrgeSurfed())
        assertEquals(50, FocusXpRules.xpForDeepReading(scorePercent = 80))
        assertEquals(10, FocusXpRules.xpForDeepReading(scorePercent = 40)) // Below 66% threshold
        assertEquals(23, FocusXpRules.xpForFocusSession(durationMinutes = 25, distractionCount = 1))
        assertEquals(15, FocusXpRules.xpForDailyFoundation())
    }

    @Test
    fun playerProgress_defaultsPreserveOldSerialization() {
        val progress = PlayerProgress()
        assertEquals(0, progress.totalFocusMinutes)
        assertEquals(0, progress.urgesSurfedCount)
        assertEquals(0, progress.circuitsCompletedCount)
        assertEquals(0, progress.sleepLogsCount)
        assertEquals(0, progress.movementLogsCount)
    }
}
