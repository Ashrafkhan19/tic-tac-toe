package com.example.tictactoe.domain.focus

import kotlin.random.Random

/**
 * Backward Memory Span Engine.
 * Tests mental manipulation within working memory (reversing active representations).
 */
class BackwardSpanEngine(
    val startingSpan: Int = 3,
    seed: Long = System.currentTimeMillis()
) {
    private val random = Random(seed)
    var currentSpan: Int = startingSpan
        private set
    var maxSpanAchieved: Int = startingSpan
        private set
    var strikes: Int = 0
        private set
    var isGameOver: Boolean = false
        private set

    private var activeSequence: List<Int> = generateSequence(currentSpan)

    fun currentSequence(): List<Int> = activeSequence

    private fun generateSequence(length: Int): List<Int> {
        return (0 until length).map { random.nextInt(1, 10) }
    }

    fun submitReverseSequence(userTaps: List<Int>): Boolean {
        if (isGameOver) return false
        val expected = activeSequence.reversed()
        val isCorrect = userTaps == expected
        if (isCorrect) {
            currentSpan++
            if (currentSpan > maxSpanAchieved) {
                maxSpanAchieved = currentSpan
            }
            strikes = 0
            activeSequence = generateSequence(currentSpan)
            return true
        } else {
            strikes++
            if (strikes >= 2) {
                isGameOver = true
            } else {
                activeSequence = generateSequence(currentSpan)
            }
            return false
        }
    }
}

