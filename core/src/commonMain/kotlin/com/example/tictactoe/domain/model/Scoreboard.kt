package com.example.tictactoe.domain.model

data class Scoreboard(
    val xWins: Int = 0,
    val oWins: Int = 0,
    val draws: Int = 0,
) {
    fun record(outcome: Outcome): Scoreboard = when (outcome) {
        is Outcome.Won if outcome.winner == Mark.X -> copy(xWins = xWins + 1)
        is Outcome.Won -> copy(oWins = oWins + 1)
        Outcome.Draw -> copy(draws = draws + 1)
        Outcome.InProgress -> this
    }
}
