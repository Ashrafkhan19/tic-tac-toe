package com.example.tictactoe.presentation

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound

/** Everything the screen renders: a thin wrapper that exposes the domain [Match] to the UI. */
data class UiState(val match: Match = Match()) {
    val game: GameState get() = match.game
    val mode: Mode get() = match.settings.mode
    val difficulty: Difficulty get() = match.settings.difficulty
    val xWins: Int get() = match.scoreboard.xWins
    val oWins: Int get() = match.scoreboard.oWins
    val draws: Int get() = match.scoreboard.draws
    val isComputerTurn: Boolean get() = match.isComputerTurn
}

sealed interface Intent {
    data class CellTapped(val index: Int) : Intent
    data class ComputerPlayed(val index: Int) : Intent
    data class ModeSelected(val mode: Mode) : Intent
    data class DifficultySelected(val difficulty: Difficulty) : Intent
    data object NewRound : Intent
    data object ResetScores : Intent
}

/** Pure `(UiState, Intent) -> UiState`: delegates every rule to a domain use case. */
class GameReducer(
    private val playHumanMove: PlayHumanMove,
    private val applyComputerMove: ApplyComputerMove,
    private val startNextRound: StartNextRound,
    private val changeSettings: ChangeSettings,
    private val resetScores: ResetScores,
) {
    fun reduce(state: UiState, intent: Intent): UiState {
        val match = state.match
        return UiState(
            when (intent) {
                is Intent.CellTapped -> playHumanMove(match, intent.index)
                is Intent.ComputerPlayed -> applyComputerMove(match, intent.index)
                is Intent.ModeSelected -> changeSettings(match, match.settings.copy(mode = intent.mode))
                is Intent.DifficultySelected -> changeSettings(match, match.settings.copy(difficulty = intent.difficulty))
                Intent.NewRound -> startNextRound(match)
                Intent.ResetScores -> resetScores(match)
            },
        )
    }
}
