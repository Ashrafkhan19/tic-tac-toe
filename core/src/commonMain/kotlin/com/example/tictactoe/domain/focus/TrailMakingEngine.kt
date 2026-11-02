package com.example.tictactoe.domain.focus

import kotlin.random.Random

data class TrailTarget(
    val label: String,
    val xRatio: Float,
    val yRatio: Float,
    val stepOrder: Int
)

/**
 * Trail Making Test (Part B: 1-A-2-B) Engine.
 * Tests cognitive flexibility, task switching, and executive function.
 */
class TrailMakingEngine(
    val targetPairsCount: Int = 8, // 1..8 and A..H (16 total targets)
    seed: Long = 42L
) {
    private val random = Random(seed)
    val allTargets: List<TrailTarget>
    var currentStep: Int = 0
        private set
    var penaltyCount: Int = 0
        private set
    var isFinished: Boolean = false
        private set

    init {
        val list = mutableListOf<TrailTarget>()
        val letters = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        var order = 0
        for (i in 1..targetPairsCount) {
            val numLabel = i.toString()
            val letLabel = letters[i - 1]
            list.add(TrailTarget(label = numLabel, xRatio = random.nextFloat() * 0.7f + 0.15f, yRatio = random.nextFloat() * 0.7f + 0.15f, stepOrder = order++))
            list.add(TrailTarget(label = letLabel, xRatio = random.nextFloat() * 0.7f + 0.15f, yRatio = random.nextFloat() * 0.7f + 0.15f, stepOrder = order++))
        }
        allTargets = list
    }

    fun currentExpectedLabel(): String? {
        if (currentStep >= allTargets.size) return null
        return allTargets[currentStep].label
    }

    fun tapTarget(label: String): Boolean {
        if (isFinished) return false
        val expected = currentExpectedLabel()
        if (label == expected) {
            currentStep++
            if (currentStep >= allTargets.size) {
                isFinished = true
            }
            return true
        } else {
            penaltyCount++
            return false
        }
    }
}

