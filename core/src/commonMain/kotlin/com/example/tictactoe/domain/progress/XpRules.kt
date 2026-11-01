package com.example.tictactoe.domain.progress

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant

object XpRules {
    const val CHALLENGE_BONUS = 50

    fun forGame(
        variant: GameVariant = GameVariant.CLASSIC_3X3,
        difficulty: Difficulty,
        result: GameResult
    ): Int = when (result) {
        GameResult.Loss -> when (variant) {
            GameVariant.CLASSIC_3X3 -> 1
            GameVariant.GRID_4X4 -> 2
            GameVariant.GRID_5X5 -> 3
            GameVariant.ULTIMATE -> 5
        }
        GameResult.Draw -> when (variant) {
            GameVariant.CLASSIC_3X3 -> when (difficulty) {
                Difficulty.Easy -> 3
                Difficulty.Medium -> 6
                Difficulty.Unbeatable -> 15
            }
            GameVariant.GRID_4X4 -> when (difficulty) {
                Difficulty.Easy -> 5
                Difficulty.Medium -> 10
                Difficulty.Unbeatable -> 20
            }
            GameVariant.GRID_5X5 -> when (difficulty) {
                Difficulty.Easy -> 8
                Difficulty.Medium -> 15
                Difficulty.Unbeatable -> 30
            }
            GameVariant.ULTIMATE -> when (difficulty) {
                Difficulty.Easy -> 15
                Difficulty.Medium -> 25
                Difficulty.Unbeatable -> 40
            }
        }
        GameResult.Win -> when (variant) {
            GameVariant.CLASSIC_3X3 -> when (difficulty) {
                Difficulty.Easy -> 10
                Difficulty.Medium -> 20
                Difficulty.Unbeatable -> 40
            }
            GameVariant.GRID_4X4 -> when (difficulty) {
                Difficulty.Easy -> 20
                Difficulty.Medium -> 35
                Difficulty.Unbeatable -> 60
            }
            GameVariant.GRID_5X5 -> when (difficulty) {
                Difficulty.Easy -> 30
                Difficulty.Medium -> 50
                Difficulty.Unbeatable -> 80
            }
            GameVariant.ULTIMATE -> when (difficulty) {
                Difficulty.Easy -> 50
                Difficulty.Medium -> 80
                Difficulty.Unbeatable -> 120
            }
        }
    }

    /** Overload for backwards compatibility with call sites that don't pass a variant. */
    fun forGame(difficulty: Difficulty, result: GameResult): Int =
        forGame(GameVariant.CLASSIC_3X3, difficulty, result)

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
