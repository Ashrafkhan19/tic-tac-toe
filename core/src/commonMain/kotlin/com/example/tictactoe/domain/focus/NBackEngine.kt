package com.example.tictactoe.domain.focus

/**
 * Core N-Back sequence validation engine.
 */
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

/**
 * State and tap tracking session wrapper for N-Back drills.
 */
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
