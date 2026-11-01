package com.example.tictactoe.domain.focus

import kotlin.math.exp

data class CognitivePillars(
    val speedPercentile: Int,
    val memoryPercentile: Int,
    val inhibitionPercentile: Int,
    val flexibilityPercentile: Int,
    val problemSolvingPercentile: Int
) {
    val overallIndex: Int
        get() = ((speedPercentile + memoryPercentile + inhibitionPercentile + flexibilityPercentile + problemSolvingPercentile) / 5)
}

object CognitiveBenchmark {
    /**
     * Logistic approximation to the standard normal Cumulative Distribution Function (CDF).
     * Maps a Z-score to a percentile between 1 and 99.
     */
    fun zToPercentile(z: Double): Int {
        val probability = 1.0 / (1.0 + exp(-1.702 * z))
        return (probability * 100).toInt().coerceIn(1, 99)
    }

    /**
     * Population Norm: Mean = 290ms, SD = 45ms.
     * Lower is better (faster reaction).
     */
    fun reactionTimePercentile(ms: Long): Int {
        if (ms <= 0L) return 50
        val z = (290.0 - ms.toDouble()) / 45.0
        return zToPercentile(z)
    }

    /**
     * Population Norm: Mean = 5.5 items/blocks, SD = 1.2.
     * Higher is better.
     */
    fun workingMemoryPercentile(span: Int): Int {
        if (span <= 0) return 50
        val z = (span.toDouble() - 5.5) / 1.2
        return zToPercentile(z)
    }

    /**
     * Population Norm: Mean = 82%, SD = 10%.
     * Higher is better.
     */
    fun inhibitionPercentile(accuracyPercent: Int): Int {
        val z = (accuracyPercent.toDouble() - 82.0) / 10.0
        return zToPercentile(z)
    }

    /**
     * Population Norm: Mean = 25.0 seconds, SD = 6.0 seconds.
     * Lower is better (faster switching).
     */
    fun flexibilityPercentile(completionSeconds: Double): Int {
        if (completionSeconds <= 0.0) return 50
        val z = (25.0 - completionSeconds) / 6.0
        return zToPercentile(z)
    }

    /**
     * Population Norm: Mean = 14 operations/min, SD = 4.0.
     * Higher is better.
     */
    fun speedMathPercentile(correctPerMin: Int): Int {
        val z = (correctPerMin.toDouble() - 14.0) / 4.0
        return zToPercentile(z)
    }

    /**
     * Population Norm: Mean = 70%, SD = 15%.
     */
    fun problemSolvingPercentile(scorePercent: Int): Int {
        val z = (scorePercent.toDouble() - 70.0) / 15.0
        return zToPercentile(z)
    }

    fun tierLabel(percentile: Int): String = when {
        percentile >= 95 -> "Top 5% Elite"
        percentile >= 85 -> "Top 15% Advanced"
        percentile >= 70 -> "Top 30% Above Average"
        percentile >= 40 -> "Average"
        else -> "Building Stamina"
    }
}
