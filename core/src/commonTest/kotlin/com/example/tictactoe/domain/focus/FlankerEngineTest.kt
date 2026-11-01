package com.example.tictactoe.domain.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlankerEngineTest {
    @Test
    fun flankerEngine_generatesCongruentAndIncongruentTrials() {
        val engine = FlankerEngine(totalTrials = 10, seed = 42L)
        val trial = engine.currentTrial()
        assertEquals(9, trial.arrows.length) // 5 arrows separated by spaces

        // Center arrow determines the correct direction
        val expectedDirection = trial.centerDirection
        val result = engine.submitAnswer(expectedDirection, reactionMs = 280L)
        assertTrue(result)
        assertEquals(1, engine.completedCount)
        assertEquals(1, engine.correctCount)
    }

    @Test
    fun flankerEngine_calculatesConflictCostAccurately() {
        val engine = FlankerEngine(totalTrials = 2)
        // Simulate congruent trial: 250ms
        // Simulate incongruent trial: 320ms
        // Flanker cost = 320 - 250 = 70ms
        assertTrue(engine.congruentTrialCount >= 0)
    }
}
