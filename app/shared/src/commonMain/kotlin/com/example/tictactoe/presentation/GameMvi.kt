package com.example.tictactoe.presentation

import com.example.tictactoe.domain.challenge.DailyChallenge
import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound
import kotlinx.datetime.LocalDate

/**
 * Everything the screen renders: the domain [Match] plus the player's saved [progress].
 * [todayChallenge] is null while the puzzle for [today] is still being generated off the main thread.
 */
data class UiState(
    val match: Match = Match(),
    val progress: PlayerProgress = PlayerProgress(),
    val today: LocalDate,
    val todayChallenge: DailyChallenge? = null,
    val inChallenge: Boolean = false,
) {
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
    data object StartDailyChallenge : Intent
    data object LeaveDailyChallenge : Intent

    /** Re-reads today's date (sent on app resume). Handled by the ViewModel, which owns the clock. */
    data object RefreshDate : Intent
}

/** Pure `(UiState, Intent) -> UiState`: delegates every game rule to a domain use case. */
class GameReducer(
    private val playHumanMove: PlayHumanMove,
    private val applyComputerMove: ApplyComputerMove,
    private val startNextRound: StartNextRound,
    private val changeSettings: ChangeSettings,
    private val resetScores: ResetScores,
) {
    fun reduce(state: UiState, intent: Intent): UiState {
        val match = state.match
        val challenge = state.todayChallenge.takeIf { state.inChallenge }
        return when (intent) {
            is Intent.CellTapped -> state.copy(match = playHumanMove(match, intent.index))
            is Intent.ComputerPlayed -> state.copy(match = applyComputerMove(match, intent.index))
            is Intent.ModeSelected ->
                if (state.inChallenge) state
                else state.copy(match = changeSettings(match, match.settings.copy(mode = intent.mode)))
            is Intent.DifficultySelected ->
                if (state.inChallenge) state
                else state.copy(match = changeSettings(match, match.settings.copy(difficulty = intent.difficulty)))
            Intent.NewRound ->
                if (challenge != null) state.copy(match = match.copy(game = challenge.game))
                else state.copy(match = startNextRound(match))
            Intent.ResetScores ->
                if (challenge != null) state.copy(match = challengeMatch(challenge))
                else state.copy(match = resetScores(match))
            Intent.StartDailyChallenge -> when (val today = state.todayChallenge) {
                null -> state
                else -> state.copy(match = challengeMatch(today), inChallenge = true)
            }
            Intent.LeaveDailyChallenge ->
                if (state.inChallenge) state.copy(match = Match(), inChallenge = false) else state
            Intent.RefreshDate -> state
        }
    }

    /** Applies a date read from the clock. Ignored while in a challenge, so its date cannot change under it. */
    fun dateRefreshed(state: UiState, today: LocalDate): UiState = when {
        state.inChallenge || today == state.today -> state
        else -> state.copy(today = today, todayChallenge = null)
    }

    /** Publishes a generated challenge if it is still for the current day and no challenge is being played. */
    fun challengeLoaded(state: UiState, challenge: DailyChallenge): UiState = when {
        state.inChallenge || challenge.date != state.today -> state
        else -> state.copy(todayChallenge = challenge)
    }

    private fun challengeMatch(challenge: DailyChallenge) =
        Match(MatchSettings(Mode.VsComputer, Difficulty.Unbeatable), game = challenge.game)
}
