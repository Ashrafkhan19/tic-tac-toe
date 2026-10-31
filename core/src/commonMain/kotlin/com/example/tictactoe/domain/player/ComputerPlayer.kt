package com.example.tictactoe.domain.player

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameState

/** Strategy for choosing the computer's next cell. Implementations must return an empty cell. */
fun interface ComputerPlayer {
    fun chooseMove(game: GameState): Int
}

/** Maps a difficulty to the strategy that plays it, so callers never depend on concrete players. */
fun interface ComputerPlayerProvider {
    fun playerFor(difficulty: Difficulty): ComputerPlayer
}
