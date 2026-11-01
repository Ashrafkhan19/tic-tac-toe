package com.example.tictactoe.domain.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeuroLabEngineTest {
    @Test
    fun stroopEngine_generatesCorrectTrialCountAndValidatesAnswers() {
        val engine = StroopEngine(totalTrials = 10, seed = 42L)
        val firstTrial = engine.currentTrial()
        
        // Correct font color submission
        val isCorrect = engine.submitAnswer(firstTrial.fontColor)
        assertTrue(isCorrect)
        assertEquals(1, engine.completedTrialsCount())
        assertEquals(1, engine.correctTrialsCount())
    }

    @Test
    fun stroopEngine_rejectsRapidDoubleTapInSameTrial() {
        val engine = StroopEngine(totalTrials = 10, seed = 42L)
        val firstTrial = engine.currentTrial()
        val firstResult = engine.submitAnswer(firstTrial.fontColor, timestampMillis = 1000L)
        assertTrue(firstResult)
        
        // Second answer within 40ms is rejected
        val doubleTap = engine.submitAnswer(firstTrial.fontColor, timestampMillis = 1040L)
        assertFalse(doubleTap)
        assertEquals(1, engine.completedTrialsCount())
    }

    @Test
    fun nBackEngine_generatesValidSequenceAndDetectsMatches() {
        val engine = NBackEngine(sequence = listOf("A", "B", "A", "C", "A"), n = 2)
        assertEquals("A", engine.currentStimulus(0))
        assertFalse(engine.isMatch(0))
        assertFalse(engine.isMatch(1))
        assertTrue(engine.isMatch(2)) // "A" matches index 0 "A"
        assertFalse(engine.isMatch(3)) // "C" != "B"
        assertTrue(engine.isMatch(4)) // "A" matches index 2 "A"
    }
}
