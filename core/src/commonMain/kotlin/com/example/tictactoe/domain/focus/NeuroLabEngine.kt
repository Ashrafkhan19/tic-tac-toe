package com.example.tictactoe.domain.focus

import kotlin.random.Random

enum class StroopColor(val displayName: String, val hexColor: Long) {
    RED("RED", 0xFFE53935),
    BLUE("BLUE", 0xFF1E88E5),
    GREEN("GREEN", 0xFF43A047),
    YELLOW("YELLOW", 0xFFFDD835)
}

data class StroopTrial(
    val index: Int,
    val word: StroopColor,
    val fontColor: StroopColor,
    val isCongruent: Boolean = word == fontColor
)

class StroopEngine(
    val totalTrials: Int = 30,
    seed: Long = System.currentTimeMillis()
) {
    private val random = Random(seed)
    private val trials: List<StroopTrial>
    private var currentIndex = 0
    private var correctCount = 0
    private var answeredCurrent = false

    init {
        val colors = StroopColor.entries.toTypedArray()
        trials = (0 until totalTrials).map { i ->
            val isCongruent = random.nextFloat() < 0.35f
            val word = colors[random.nextInt(colors.size)]
            val fontColor = if (isCongruent) {
                word
            } else {
                val nonMatches = colors.filter { it != word }
                nonMatches[random.nextInt(nonMatches.size)]
            }
            StroopTrial(index = i, word = word, fontColor = fontColor, isCongruent = isCongruent)
        }
    }

    private var lastAnswerTimestamp = 0L

    fun currentTrial(): StroopTrial = trials[currentIndex.coerceAtMost(trials.lastIndex)]
    fun isFinished(): Boolean = currentIndex >= totalTrials
    fun completedTrialsCount(): Int = currentIndex
    fun correctTrialsCount(): Int = correctCount
    fun accuracyPercent(): Int = if (currentIndex == 0) 0 else ((correctCount * 100) / currentIndex)

    fun submitAnswer(chosenColor: StroopColor, timestampMillis: Long = 0L): Boolean {
        if (isFinished()) return false
        if (timestampMillis > 0L && timestampMillis - lastAnswerTimestamp < 100L) {
            return false // Debounce rapid double-tap
        }
        if (timestampMillis > 0L) {
            lastAnswerTimestamp = timestampMillis
        }
        val trial = currentTrial()
        val correct = chosenColor == trial.fontColor
        if (correct) correctCount++
        currentIndex++
        return correct
    }
}

class NBackEngine(
    val sequence: List<String>,
    val n: Int = 2
) {
    fun currentStimulus(index: Int): String = sequence[index]
    fun isMatch(index: Int): Boolean {
        if (index < n || index >= sequence.size) return false
        return sequence[index] == sequence[index - n]
    }
}
