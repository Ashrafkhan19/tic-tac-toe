package com.example.tictactoe.domain.focus

import kotlin.random.Random

/**
 * Corsi Block Tapping Spatial Working Memory Engine.
 * Tests and trains spatial working memory capacity through sequence reproduction.
 */
class CorsiBlockEngine(
    val startingSpan: Int = 3,
    val maxGridSize: Int = 9,
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
        val list = mutableListOf<Int>()
        var last = -1
        while (list.size < length) {
            val next = random.nextInt(maxGridSize)
            if (next != last) {
                list.add(next)
                last = next
            }
        }
        return list
    }

    fun submitSequence(userTaps: List<Int>): Boolean {
        if (isGameOver) return false
        val isCorrect = userTaps == activeSequence
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
