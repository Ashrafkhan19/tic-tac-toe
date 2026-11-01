package com.example.tictactoe.domain.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpandedGamesDomainTest {
    @Test
    fun goNoGoEngine_evaluatesGoAndNoGoCorrectly() {
        val engine = GoNoGoEngine(totalTrials = 10, seed = 123L)
        assertEquals(10, engine.totalTrials)

        val trial = engine.currentTrial()
        if (trial.isGo) {
            val tapResult = engine.recordTap(timestampMillis = 200L)
            assertTrue(tapResult)
        } else {
            val tapResult = engine.recordTap(timestampMillis = 200L)
            assertFalse(tapResult) // Tapping on No-Go is an error
        }
    }

    @Test
    fun corsiBlockEngine_advancesSpanOnSuccess() {
        val engine = CorsiBlockEngine(startingSpan = 3, seed = 42L)
        assertEquals(3, engine.currentSpan)

        val sequence = engine.currentSequence()
        assertEquals(3, sequence.size)

        // Submit matching sequence
        val success = engine.submitSequence(sequence)
        assertTrue(success)
        assertEquals(4, engine.currentSpan)
        assertEquals(4, engine.maxSpanAchieved)
    }

    @Test
    fun corsiBlockEngine_endsAfterTwoStrikes() {
        val engine = CorsiBlockEngine(startingSpan = 3, seed = 42L)
        val wrongSequence = listOf(99, 99, 99)

        assertFalse(engine.submitSequence(wrongSequence))
        assertFalse(engine.isGameOver) // 1 strike, still alive

        assertFalse(engine.submitSequence(wrongSequence))
        assertTrue(engine.isGameOver) // 2 strikes, game over
    }

    @Test
    fun nBackSession_tracksHitsMissesAndFalseAlarms() {
        // Sequence: A, B, A, C, A
        val sequence = listOf("A", "B", "A", "C", "A")
        val session = NBackSession(sequence = sequence, n = 2)

        // Index 0: "A" (no match possible)
        session.recordTap(0) // False alarm
        // Index 1: "B" (no match possible)
        // Index 2: "A" matches index 0 -> tap (Hit)
        session.recordTap(2)
        // Index 3: "C" (no match) -> no tap
        // Index 4: "A" matches index 2 -> no tap (Miss)

        assertEquals(1, session.hits)
        assertEquals(1, session.falseAlarms)
        assertEquals(1, session.misses)
    }
}

