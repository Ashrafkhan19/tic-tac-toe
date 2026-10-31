package com.example.tictactoe.domain.progress

import com.example.tictactoe.domain.model.Difficulty

object XpRules {
    const val CHALLENGE_BONUS = 50

    fun forGame(difficulty: Difficulty, result: GameResult): Int = when (result) {
        GameResult.Loss -> 1
        GameResult.Win -> when (difficulty) {
            Difficulty.Easy -> 10
            Difficulty.Medium -> 20
            Difficulty.Unbeatable -> 40
        }
        GameResult.Draw -> when (difficulty) {
            Difficulty.Easy -> 3
            Difficulty.Medium -> 6
            Difficulty.Unbeatable -> 15
        }
    }

    /** Total XP needed to reach [level]: 50 * n * (n + 1) with n = level - 1 (level 2 = 100, level 3 = 300). */
    fun xpToReach(level: Int): Int {
        val n = (level - 1).coerceAtLeast(0)
        return 50 * n * (n + 1)
    }

    fun levelFor(xp: Int): Int {
        var level = 1
        while (xpToReach(level + 1) <= xp) level++
        return level
    }
}
