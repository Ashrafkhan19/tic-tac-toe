package com.example.tictactoe.domain.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdvancedGamesTest {
    @Test
    fun trailMakingEngine_validatesAlternatingSequence() {
        val engine = TrailMakingEngine(targetPairsCount = 4) // 1, A, 2, B, 3, C, 4, D
        val targets = engine.allTargets
        assertEquals(8, targets.size)
        assertEquals("1", targets[0].label)
        assertEquals("A", targets[1].label)
        assertEquals("2", targets[2].label)
        assertEquals("B", targets[3].label)

        // First tap: "1"
        assertTrue(engine.tapTarget("1"))
        // Wrong tap: "2" instead of "A"
        assertFalse(engine.tapTarget("2"))
        assertEquals(1, engine.penaltyCount)

        // Correct tap: "A"
        assertTrue(engine.tapTarget("A"))
    }

    @Test
    fun speedMathEngine_evaluatesCalculations() {
        val engine = SpeedMathEngine(seed = 42L)
        val question = engine.currentQuestion()
        val isCorrect = engine.submitAnswer(question.correctAnswer)
        assertTrue(isCorrect)
        assertEquals(1, engine.correctCount)
        assertEquals(1, engine.totalAnswered)
    }

    @Test
    fun backwardSpanEngine_validatesReverseOrder() {
        val engine = BackwardSpanEngine(startingSpan = 3, seed = 123L)
        val seq = engine.currentSequence() // e.g. [4, 7, 1]
        val reverseSeq = seq.reversed()

        assertTrue(engine.submitReverseSequence(reverseSeq))
        assertEquals(4, engine.currentSpan)
    }

    @Test
    fun matrixLogicEngine_evaluatesPuzzles() {
        val engine = MatrixLogicEngine()
        val puzzle = engine.allPuzzles.first()
        val correct = engine.submitAnswer(puzzle.id, puzzle.correctOptionIndex)
        assertTrue(correct)
    }
}

