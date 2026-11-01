package com.example.tictactoe.domain.focus

import kotlin.random.Random

enum class ArrowDirection {
    LEFT,
    RIGHT
}

data class FlankerTrial(
    val index: Int,
    val arrows: String,
    val centerDirection: ArrowDirection,
    val isCongruent: Boolean
)

class FlankerEngine(
    val totalTrials: Int = 30,
    seed: Long = System.currentTimeMillis()
) {
    private val random = Random(seed)
    private val trials: List<FlankerTrial>
    var currentIndex: Int = 0
        private set
    var completedCount: Int = 0
        private set
    var correctCount: Int = 0
        private set

    var congruentTrialCount: Int = 0
        private set
    private var congruentReactionSum: Long = 0L

    var incongruentTrialCount: Int = 0
        private set
    private var incongruentReactionSum: Long = 0L

    init {
        trials = (0 until totalTrials).map { i ->
            val centerIsLeft = random.nextBoolean()
            val isCongruent = random.nextFloat() < 0.5f // 50% congruent, 50% incongruent
            val flankerIsLeft = if (isCongruent) centerIsLeft else !centerIsLeft

            val cChar = if (centerIsLeft) "<" else ">"
            val fChar = if (flankerIsLeft) "<" else ">"
            val arrowsString = "$fChar $fChar $cChar $fChar $fChar"

            FlankerTrial(
                index = i,
                arrows = arrowsString,
                centerDirection = if (centerIsLeft) ArrowDirection.LEFT else ArrowDirection.RIGHT,
                isCongruent = isCongruent
            )
        }
    }

    fun currentTrial(): FlankerTrial = trials[currentIndex.coerceAtMost(trials.lastIndex)]
    fun isFinished(): Boolean = currentIndex >= totalTrials

    fun submitAnswer(direction: ArrowDirection, reactionMs: Long): Boolean {
        if (isFinished()) return false
        val trial = currentTrial()
        val correct = direction == trial.centerDirection
        if (correct) {
            correctCount++
            if (trial.isCongruent) {
                congruentTrialCount++
                congruentReactionSum += reactionMs
            } else {
                incongruentTrialCount++
                incongruentReactionSum += reactionMs
            }
        }
        completedCount++
        currentIndex++
        return correct
    }

    fun accuracyPercent(): Int {
        if (completedCount == 0) return 0
        return (correctCount * 100) / completedCount
    }

    /**
     * Flanker Conflict Cost = Mean Incongruent Reaction Time - Mean Congruent Reaction Time.
     * Lower conflict cost (closer to 0ms) means superior visual noise suppression.
     */
    fun conflictCostMs(): Long {
        if (congruentTrialCount == 0 || incongruentTrialCount == 0) return 45L
        val meanCongruent = congruentReactionSum / congruentTrialCount
        val meanIncongruent = incongruentReactionSum / incongruentTrialCount
        return (meanIncongruent - meanCongruent).coerceAtLeast(0L)
    }
}
