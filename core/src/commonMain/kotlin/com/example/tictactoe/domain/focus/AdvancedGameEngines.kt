package com.example.tictactoe.domain.focus

import kotlin.random.Random

// --- 1. TRAIL MAKING (TASK SWITCHING & FLEXIBILITY) ---

data class TrailTarget(
    val label: String,
    val xRatio: Float,
    val yRatio: Float,
    val stepOrder: Int
)

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

// --- 2. SPEED ARITHMETIC FLOW ENGINE ---

data class MathQuestion(
    val prompt: String,
    val correctAnswer: Int,
    val options: List<Int>
)

class SpeedMathEngine(
    seed: Long = System.currentTimeMillis()
) {
    private val random = Random(seed)
    var correctCount: Int = 0
        private set
    var totalAnswered: Int = 0
        private set

    private var activeQuestion: MathQuestion = generateQuestion()

    fun currentQuestion(): MathQuestion = activeQuestion

    fun submitAnswer(chosen: Int): Boolean {
        val isCorrect = chosen == activeQuestion.correctAnswer
        if (isCorrect) correctCount++
        totalAnswered++
        activeQuestion = generateQuestion()
        return isCorrect
    }

    private fun generateQuestion(): MathQuestion {
        val op = random.nextInt(3) // 0: +, 1: -, 2: *
        val a: Int
        val b: Int
        val ans: Int
        val prompt: String

        when (op) {
            0 -> {
                a = random.nextInt(12, 60)
                b = random.nextInt(12, 60)
                ans = a + b
                prompt = "$a + $b"
            }
            1 -> {
                a = random.nextInt(30, 99)
                b = random.nextInt(10, a)
                ans = a - b
                prompt = "$a - $b"
            }
            else -> {
                a = random.nextInt(3, 12)
                b = random.nextInt(4, 12)
                ans = a * b
                prompt = "$a × $b"
            }
        }

        val distractors = setOf(ans + random.nextInt(1, 6), (ans - random.nextInt(1, 6)).coerceAtLeast(0))
        val options = (distractors + ans).toList().shuffled(random)
        return MathQuestion(prompt = prompt, correctAnswer = ans, options = options)
    }
}

// --- 3. BACKWARD MEMORY SPAN ENGINE ---

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

// --- 4. VISUAL MATRIX LOGIC ENGINE ---

data class MatrixPuzzle(
    val id: String,
    val title: String,
    val ruleDescription: String,
    val gridIcons: List<String>, // 4 icons for 2x2 grid (top-left, top-right, bottom-left, empty "?")
    val options: List<String>,
    val correctOptionIndex: Int
)

class MatrixLogicEngine {
    val allPuzzles = listOf(
        MatrixPuzzle(
            id = "rotation-1",
            title = "Arrow Rotation",
            ruleDescription = "Each shape rotates clockwise by 90 degrees.",
            gridIcons = listOf("↑", "→", "↓", "?"),
            options = listOf("←", "→", "↑", "↓"),
            correctOptionIndex = 0
        ),
        MatrixPuzzle(
            id = "progression-1",
            title = "Dot Progression",
            ruleDescription = "Each step adds one circle.",
            gridIcons = listOf("●", "●●", "●●●", "?"),
            options = listOf("●●", "●●●●", "●", "●●●"),
            correctOptionIndex = 1
        ),
        MatrixPuzzle(
            id = "morph-1",
            title = "Shape Progression",
            ruleDescription = "Sides increase: Triangle (3) -> Square (4) -> Pentagon (5) -> ?",
            gridIcons = listOf("▲", "■", "⬟", "?"),
            options = listOf("●", "⬢ (Hexagon)", "▲", "◆"),
            correctOptionIndex = 1
        ),
        MatrixPuzzle(
            id = "shading-1",
            title = "Polarity Inversion",
            ruleDescription = "Opposite shapes invert fill state.",
            gridIcons = listOf("○", "●", "□", "?"),
            options = listOf("■", "○", "▲", "△"),
            correctOptionIndex = 0
        )
    )

    fun submitAnswer(puzzleId: String, chosenIndex: Int): Boolean {
        val puzzle = allPuzzles.find { it.id == puzzleId } ?: return false
        return chosenIndex == puzzle.correctOptionIndex
    }
}
