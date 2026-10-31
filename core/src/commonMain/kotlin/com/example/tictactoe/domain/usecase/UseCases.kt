package com.example.tictactoe.domain.usecase

import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.player.ComputerPlayerProvider

/** A human taps a cell. Ignored while it is the computer's turn. */
class PlayHumanMove {
    operator fun invoke(match: Match, index: Int): Match =
        if (match.isComputerTurn) match else match.play(index)
}

/** Picks (but does not apply) the computer's move for the current position. */
class ChooseComputerMove(private val players: ComputerPlayerProvider) {
    operator fun invoke(match: Match): Int =
        players.playerFor(match.settings.difficulty).chooseMove(match.game)
}

/** Applies a previously chosen computer move. Ignored unless it is the computer's turn. */
class ApplyComputerMove {
    operator fun invoke(match: Match, index: Int): Match =
        if (match.isComputerTurn) match.play(index) else match
}

class StartNextRound {
    operator fun invoke(match: Match): Match = match.nextRound()
}

class ChangeSettings {
    operator fun invoke(match: Match, settings: MatchSettings): Match = match.withSettings(settings)
}

class ResetScores {
    operator fun invoke(match: Match): Match = match.withSettings(match.settings)
}
