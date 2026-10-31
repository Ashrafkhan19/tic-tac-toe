package com.example.tictactoe.domain.progress

import com.example.tictactoe.domain.model.Difficulty
import kotlinx.serialization.Serializable

enum class GameResult { Win, Draw, Loss }

@Serializable
data class DifficultyStats(
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
) {
    fun record(result: GameResult): DifficultyStats = when (result) {
        GameResult.Win -> copy(wins = wins + 1)
        GameResult.Draw -> copy(draws = draws + 1)
        GameResult.Loss -> copy(losses = losses + 1)
    }
}

@Serializable
data class PlayerProgress(
    val version: Int = 1,
    val xp: Int = 0,
    val stats: Map<Difficulty, DifficultyStats> = emptyMap(),
    val streak: StreakState = StreakState(),
)
