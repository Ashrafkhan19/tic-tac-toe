package com.example.tictactoe.presentation

import com.example.tictactoe.domain.challenge.DailyChallenge
import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound
import kotlinx.datetime.LocalDate

fun currentTurn(board: GridBoard): Mark =
    if (board.cells.count { it != null } % 2 == 0) Mark.X else Mark.O

fun currentTurn(board: UltimateBoard): Mark =
    if (board.subBoards.sumOf { sb -> sb.cells.count { it != null } } % 2 == 0) Mark.X else Mark.O

/**
 * Everything the screen renders: the domain [Match] plus the player's saved [progress].
 * [todayChallenge] is null while the puzzle for [today] is still being generated off the main thread.
 * [freePlay] holds the free-play match set aside while [inChallenge], restored on leaving.
 */
data class UiState(
    val match: Match = Match(),
    val progress: PlayerProgress = PlayerProgress(),
    val today: LocalDate,
    val todayChallenge: DailyChallenge? = null,
    val inChallenge: Boolean = false,
    val freePlay: Match? = null,
    val variant: GameVariant = GameVariant.CLASSIC_3X3,
    val gridBoard: GridBoard? = null,
    val ultimateBoard: UltimateBoard? = null,
) {
    val game: GameState get() = match.game
    val mode: Mode get() = match.settings.mode
    val difficulty: Difficulty get() = match.settings.difficulty
    val xWins: Int get() = match.scoreboard.xWins
    val oWins: Int get() = match.scoreboard.oWins
    val draws: Int get() = match.scoreboard.draws

    val isComputerTurn: Boolean
        get() = when (variant) {
            GameVariant.CLASSIC_3X3 -> match.isComputerTurn
            GameVariant.GRID_4X4, GameVariant.GRID_5X5 -> {
                val board = gridBoard ?: return false
                mode == Mode.VsComputer &&
                    currentTurn(board) == Mark.O &&
                    board.outcome() is Outcome.InProgress
            }
            GameVariant.ULTIMATE -> {
                val board = ultimateBoard ?: return false
                mode == Mode.VsComputer &&
                    currentTurn(board) == Mark.O &&
                    board.outcome() is Outcome.InProgress
            }
        }

    val isGameOver: Boolean
        get() = when (variant) {
            GameVariant.CLASSIC_3X3 -> game.isOver
            GameVariant.GRID_4X4, GameVariant.GRID_5X5 -> gridBoard?.outcome() !is Outcome.InProgress
            GameVariant.ULTIMATE -> ultimateBoard?.outcome() !is Outcome.InProgress
        }

    val currentOutcome: Outcome
        get() = when (variant) {
            GameVariant.CLASSIC_3X3 -> game.outcome
            GameVariant.GRID_4X4, GameVariant.GRID_5X5 -> gridBoard?.outcome() ?: Outcome.InProgress
            GameVariant.ULTIMATE -> ultimateBoard?.outcome() ?: Outcome.InProgress
        }

    /** Today's challenge is already won. `>=` so a clock moved backwards never re-offers a puzzle that cannot be credited. */
    val dailyChallengeDone: Boolean
        get() {
            val challengeDate = todayChallenge?.date ?: return false
            val last = progress.streak.lastCompleted ?: return false
            return last >= challengeDate
        }
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
    data class SelectVariant(val variant: GameVariant) : Intent
    data class PlaceVariantMove(val move: GameMove) : Intent

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
                else {
                    val newMatch = changeSettings(match, match.settings.copy(mode = intent.mode))
                    resetForVariant(state.copy(match = newMatch), state.variant)
                }
            is Intent.DifficultySelected ->
                if (state.inChallenge) state
                else {
                    val newMatch = changeSettings(match, match.settings.copy(difficulty = intent.difficulty))
                    resetForVariant(state.copy(match = newMatch), state.variant)
                }
            Intent.NewRound ->
                if (challenge != null) state.copy(match = match.copy(game = challenge.game))
                else when (state.variant) {
                    GameVariant.CLASSIC_3X3 -> state.copy(match = startNextRound(match))
                    else -> resetForVariant(state, state.variant)
                }
            Intent.ResetScores ->
                if (challenge != null) state.copy(match = challengeMatch(challenge))
                else when (state.variant) {
                    GameVariant.CLASSIC_3X3 -> state.copy(match = resetScores(match))
                    else -> resetForVariant(state.copy(match = resetScores(match)), state.variant)
                }
            Intent.StartDailyChallenge -> when (val today = state.todayChallenge) {
                null -> state
                else -> state.copy(
                    match = challengeMatch(today),
                    inChallenge = true,
                    freePlay = if (state.inChallenge) state.freePlay else match,
                    variant = GameVariant.CLASSIC_3X3,
                    gridBoard = null,
                    ultimateBoard = null,
                )
            }
            Intent.LeaveDailyChallenge ->
                if (state.inChallenge) {
                    val restored = state.freePlay ?: Match()
                    resetForVariant(
                        state.copy(
                            match = restored,
                            inChallenge = false,
                            freePlay = null,
                            variant = restored.settings.variant,
                        ),
                        restored.settings.variant,
                    )
                } else state
            Intent.RefreshDate -> state
            is Intent.SelectVariant ->
                if (state.inChallenge) state
                else {
                    val newMatch = changeSettings(match, match.settings.copy(variant = intent.variant))
                    resetForVariant(state.copy(match = newMatch, variant = intent.variant), intent.variant)
                }
            is Intent.PlaceVariantMove -> applyVariantMove(state, intent.move)
        }
    }

    private fun resetForVariant(state: UiState, variant: GameVariant): UiState = when (variant) {
        GameVariant.CLASSIC_3X3 -> state.copy(
            variant = variant,
            gridBoard = null,
            ultimateBoard = null,
        )
        GameVariant.GRID_4X4 -> state.copy(
            variant = variant,
            gridBoard = GridBoard(size = 4, winLength = 4),
            ultimateBoard = null,
        )
        GameVariant.GRID_5X5 -> state.copy(
            variant = variant,
            gridBoard = GridBoard(size = 5, winLength = 4),
            ultimateBoard = null,
        )
        GameVariant.ULTIMATE -> state.copy(
            variant = variant,
            gridBoard = null,
            ultimateBoard = UltimateBoard(),
        )
    }

    private fun applyVariantMove(state: UiState, move: GameMove): UiState {
        when (move) {
            is GameMove.Grid -> {
                val board = state.gridBoard ?: return state
                if (board.outcome() !is Outcome.InProgress) return state
                if (move.index !in board.cells.indices || board.cells[move.index] != null) return state
                val turn = currentTurn(board)
                val nextBoard = board.place(move.index, turn)
                val nextOutcome = nextBoard.outcome()
                val nextScoreboard = if (nextOutcome !is Outcome.InProgress) {
                    state.match.scoreboard.record(nextOutcome)
                } else state.match.scoreboard
                return state.copy(
                    gridBoard = nextBoard,
                    match = state.match.copy(scoreboard = nextScoreboard),
                )
            }
            is GameMove.Ultimate -> {
                val board = state.ultimateBoard ?: return state
                if (board.outcome() !is Outcome.InProgress) return state
                if (move !in board.legalMoves()) return state
                val turn = currentTurn(board)
                val nextBoard = board.place(move, turn)
                val nextOutcome = nextBoard.outcome()
                val nextScoreboard = if (nextOutcome !is Outcome.InProgress) {
                    state.match.scoreboard.record(nextOutcome)
                } else state.match.scoreboard
                return state.copy(
                    ultimateBoard = nextBoard,
                    match = state.match.copy(scoreboard = nextScoreboard),
                )
            }
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
