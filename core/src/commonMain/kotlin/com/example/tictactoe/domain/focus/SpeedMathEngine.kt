package com.example.tictactoe.domain.focus

import kotlin.random.Random

data class MathQuestion(
    val prompt: String,
    val correctAnswer: Int,
    val options: List<Int>
)

/**
 * Rapid Mental Arithmetic Flow Engine.
 * Tests working memory throughput and numerical fact retrieval.
 */
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
