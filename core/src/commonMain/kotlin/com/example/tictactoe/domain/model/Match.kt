package com.example.tictactoe.domain.model

data class MatchSettings(
    val mode: Mode = Mode.VsComputer,
    val difficulty: Difficulty = Difficulty.Unbeatable,
    val variant: GameVariant = GameVariant.CLASSIC_3X3,
)

/** A series of rounds: settings, the current round, the running score and who opened this round. */
data class Match(
    val settings: MatchSettings = MatchSettings(),
    val game: GameState = GameState(),
    val scoreboard: Scoreboard = Scoreboard(),
    val starter: Mark = Mark.X,
) {
    /** In vs-computer mode the human is X and the computer is O. */
    val isComputerTurn: Boolean
        get() = settings.mode == Mode.VsComputer && game.turn == Mark.O && !game.isOver

    /** Plays [index] for whoever's turn it is and records the result if the round ends. */
    fun play(index: Int): Match {
        val next = game.play(index)
        if (next === game) return this
        return copy(game = next, scoreboard = scoreboard.record(next.outcome))
    }

    /** New round with the opening player alternated; scores are kept. */
    fun nextRound(): Match {
        val opener = starter.other
        return copy(game = GameState(turn = opener), starter = opener)
    }

    /** Fresh match with new settings; scores are cleared. */
    fun withSettings(settings: MatchSettings): Match = Match(settings = settings)
}
