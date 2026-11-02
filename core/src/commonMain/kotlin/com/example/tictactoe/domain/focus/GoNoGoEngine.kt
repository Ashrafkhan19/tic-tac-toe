package com.example.tictactoe.domain.focus

import kotlin.random.Random

data class GoNoGoTrial(
    val index: Int,
    val isGo: Boolean,
    val durationMillis: Long = 850L
)

/**
 * Go / No-Go Motor Inhibition Engine.
 * Trains the striatal-prefrontal "stop" pathway under speed pressure.
 */
class GoNoGoEngine(
    val totalTrials: Int = 25,
    seed: Long = System.currentTimeMillis()
) {
    private val random = Random(seed)
    private val trials: List<GoNoGoTrial>
    var currentIndex: Int = 0
        private set
    var hits: Int = 0
        private set
    var commissionErrors: Int = 0 // Tapped on No-Go
        private set
    var omissionErrors: Int = 0 // Failed to tap on Go
        private set
    private var totalReactionTimeMillis: Long = 0L

    init {
        trials = (0 until totalTrials).map { i ->
            val isGo = random.nextFloat() < 0.75f // 75% Go, 25% No-Go
            GoNoGoTrial(index = i, isGo = isGo)
        }
    }

    fun currentTrial(): GoNoGoTrial = trials[currentIndex.coerceAtMost(trials.lastIndex)]
    fun isFinished(): Boolean = currentIndex >= totalTrials

    fun recordTap(timestampMillis: Long): Boolean {
        if (isFinished()) return false
        val trial = currentTrial()
        val correct: Boolean
        if (trial.isGo) {
            hits++
            totalReactionTimeMillis += timestampMillis
            correct = true
        } else {
            commissionErrors++
            correct = false
        }
        currentIndex++
        return correct
    }

    fun recordTimeout(): Boolean {
        if (isFinished()) return false
        val trial = currentTrial()
        val correct: Boolean
        if (trial.isGo) {
            omissionErrors++
            correct = false
        } else {
            // Correctly refrained from tapping No-Go
            correct = true
        }
        currentIndex++
        return correct
    }

    fun accuracyPercent(): Int {
        if (currentIndex == 0) return 0
        val totalErrors = commissionErrors + omissionErrors
        val correctCount = (currentIndex - totalErrors).coerceAtLeast(0)
        return (correctCount * 100) / currentIndex
    }

    fun averageReactionTimeMillis(): Long {
        return if (hits == 0) 0L else (totalReactionTimeMillis / hits)
    }
}

