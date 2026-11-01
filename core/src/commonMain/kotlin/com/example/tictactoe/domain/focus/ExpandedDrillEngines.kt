package com.example.tictactoe.domain.focus

import kotlin.random.Random

// --- 1. GO / NO-GO MOTOR INHIBITION ENGINE ---

data class GoNoGoTrial(
    val index: Int,
    val isGo: Boolean,
    val durationMillis: Long = 850L
)

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

// --- 2. CORSI BLOCK SPATIAL WORKING MEMORY ENGINE ---

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

// --- 3. N-BACK SESSION WRAPPER ---

class NBackSession(
    val sequence: List<String>,
    val n: Int = 2
) {
    private val engine = NBackEngine(sequence = sequence, n = n)
    private val tappedIndices = mutableSetOf<Int>()

    var hits: Int = 0
        private set
    var falseAlarms: Int = 0
        private set

    val misses: Int
        get() = sequence.indices.count { engine.isMatch(it) && it !in tappedIndices }

    fun isMatchAt(index: Int): Boolean = engine.isMatch(index)

    fun recordTap(index: Int): Boolean {
        if (index in tappedIndices || index >= sequence.size) return false
        tappedIndices.add(index)
        val isMatch = engine.isMatch(index)
        if (isMatch) {
            hits++
        } else {
            falseAlarms++
        }
        return isMatch
    }

    fun accuracyPercent(): Int {
        val totalTargetMatches = (hits + misses).coerceAtLeast(1)
        val score = ((hits.toDouble() / totalTargetMatches) * 100 - (falseAlarms * 10)).toInt()
        return score.coerceIn(0, 100)
    }
}
