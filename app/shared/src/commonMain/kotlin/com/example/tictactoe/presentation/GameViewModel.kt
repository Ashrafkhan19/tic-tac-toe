package com.example.tictactoe.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tictactoe.domain.challenge.DailyChallengeGenerator
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.ProgressRepository
import com.example.tictactoe.domain.time.TodayProvider
import com.example.tictactoe.domain.usecase.ChooseComputerMove
import com.example.tictactoe.domain.usecase.CompleteDailyChallenge
import com.example.tictactoe.domain.usecase.RecordGameResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class GameViewModel(
    private val reducer: GameReducer,
    private val chooseComputerMove: ChooseComputerMove,
    private val repository: ProgressRepository,
    private val recordGameResult: RecordGameResult,
    private val completeDailyChallenge: CompleteDailyChallenge,
    private val today: TodayProvider,
    private val challenges: DailyChallengeGenerator,
    private val computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val thinkTimeMs: Long = DEFAULT_THINK_TIME_MS,
) : ViewModel() {
    // The first frame gets a playable board; the daily challenge arrives once generated.
    private val _state = MutableStateFlow(UiState(progress = repository.load(), today = today.today()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var computerJob: Job? = null
    private var challengeJob: Job? = null

    init {
        loadChallengeFor(_state.value.today)
    }

    fun onIntent(intent: Intent) {
        when (intent) {
            Intent.RefreshDate -> refreshDate()
            Intent.LeaveDailyChallenge -> {
                computerJob?.cancel()
                dispatch(intent)
                // A date change deferred during the challenge is applied now.
                refreshDate()
            }
            else -> {
                // Any new input invalidates a pending computer move.
                computerJob?.cancel()
                dispatch(intent)
            }
        }
    }

    private fun dispatch(intent: Intent) {
        val before = _state.value
        var next = reducer.reduce(before, intent)
        // Record only the transition not-over -> over, so re-dispatching a finished state never records twice.
        if (!before.game.isOver && next.game.isOver && before.inChallenge == next.inChallenge) {
            val progress = progressAfterRound(next)
            if (progress != next.progress) {
                repository.save(progress)
                next = next.copy(progress = progress)
            }
        }
        _state.value = next
        scheduleComputerMoveIfNeeded()
    }

    private fun progressAfterRound(state: UiState): PlayerProgress {
        val outcome = state.game.outcome
        if (!state.inChallenge) return recordGameResult(state.progress, state.match.settings, outcome)
        val challenge = state.todayChallenge ?: return state.progress
        val xWon = outcome is Outcome.Won && outcome.winner == Mark.X
        // Credited to the challenge's own date, not the clock at finish time.
        return if (xWon) completeDailyChallenge(state.progress, challenge.date) else state.progress
    }

    private fun refreshDate() {
        val before = _state.value
        val after = reducer.dateRefreshed(before, today.today())
        if (after == before) return
        _state.value = after
        loadChallengeFor(after.today)
    }

    private fun loadChallengeFor(date: LocalDate) {
        challengeJob?.cancel()
        challengeJob = viewModelScope.launch {
            val challenge = withContext(computerDispatcher) { challenges.forDate(date) }
            _state.value = reducer.challengeLoaded(_state.value, challenge)
        }
    }

    private fun scheduleComputerMoveIfNeeded() {
        val current = _state.value
        if (!current.isComputerTurn) return
        computerJob = viewModelScope.launch {
            delay(thinkTimeMs)
            val move = withContext(computerDispatcher) { chooseComputerMove(current.match) }
            dispatch(Intent.ComputerPlayed(move))
        }
    }

    companion object {
        const val DEFAULT_THINK_TIME_MS = 450L
    }
}
