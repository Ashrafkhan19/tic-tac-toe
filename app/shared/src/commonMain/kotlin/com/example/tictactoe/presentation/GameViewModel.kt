package com.example.tictactoe.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tictactoe.domain.challenge.DailyChallengeGenerator
import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.player.DefaultComputerPlayerProvider
import com.example.tictactoe.domain.player.HeuristicGridAI
import com.example.tictactoe.domain.player.UltimateAI
import com.example.tictactoe.domain.progress.GameResult
import com.example.tictactoe.domain.progress.InMemoryProgressRepository
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.ProgressRepository
import com.example.tictactoe.domain.time.TodayProvider
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.ChooseComputerMove
import com.example.tictactoe.domain.usecase.CompleteDailyChallenge
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.RecordGameResult
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound
import kotlinx.coroutines.CancellationException
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

    constructor(
        progressRepository: ProgressRepository = InMemoryProgressRepository(),
        todayProvider: TodayProvider = object : TodayProvider { override fun today(): LocalDate = LocalDate(2026, 11, 1) },
        reducer: GameReducer = GameReducer(
            PlayHumanMove(),
            ApplyComputerMove(),
            StartNextRound(),
            ChangeSettings(),
            ResetScores(),
        ),
        chooseComputerMove: ChooseComputerMove = ChooseComputerMove(DefaultComputerPlayerProvider()),
        recordGameResult: RecordGameResult = RecordGameResult(),
        completeDailyChallenge: CompleteDailyChallenge = CompleteDailyChallenge(),
        challenges: DailyChallengeGenerator = DailyChallengeGenerator(),
        computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
        thinkTimeMs: Long = DEFAULT_THINK_TIME_MS,
    ) : this(
        reducer = reducer,
        chooseComputerMove = chooseComputerMove,
        repository = progressRepository,
        recordGameResult = recordGameResult,
        completeDailyChallenge = completeDailyChallenge,
        today = todayProvider,
        challenges = challenges,
        computerDispatcher = computerDispatcher,
        thinkTimeMs = thinkTimeMs,
    )

    // The first frame gets a playable board; the daily challenge arrives once generated.
    private val _state = MutableStateFlow(UiState(progress = repository.load(), today = today.today()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var computerJob: Job? = null
    private var challengeJob: Job? = null

    /** Play was tapped while the (new day's) challenge was still generating: start it once it arrives. */
    private var startWhenLoaded = false

    init {
        loadChallengeFor(_state.value.today)
    }

    fun send(intent: Intent) = onIntent(intent)

    fun onIntent(intent: Intent) {
        if (intent != Intent.RefreshDate) startWhenLoaded = false
        when (intent) {
            Intent.RefreshDate -> refreshDate()
            Intent.StartDailyChallenge -> {
                computerJob?.cancel()
                refreshDate()
                if (_state.value.todayChallenge == null) startWhenLoaded = true else dispatch(intent)
            }
            Intent.LeaveDailyChallenge -> {
                computerJob?.cancel()
                dispatch(intent)
                refreshDate()
            }
            is Intent.PlaceVariantMove -> {
                if (_state.value.isComputerTurn) return
                computerJob?.cancel()
                dispatch(intent)
            }
            else -> {
                computerJob?.cancel()
                dispatch(intent)
            }
        }
    }

    private fun dispatch(intent: Intent) {
        val before = _state.value
        var next = reducer.reduce(before, intent)
        // Record only the transition not-over -> over, so re-dispatching a finished state never records twice.
        if (!before.isGameOver && next.isGameOver && before.inChallenge == next.inChallenge) {
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
        val outcome = state.currentOutcome
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
            val challenge = try {
                withContext(computerDispatcher) { challenges.forDate(date) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // No banner today rather than a crash; free play is unaffected.
                startWhenLoaded = false
                return@launch
            }
            _state.value = reducer.challengeLoaded(_state.value, challenge)
            if (startWhenLoaded && _state.value.todayChallenge != null) {
                startWhenLoaded = false
                dispatch(Intent.StartDailyChallenge)
            }
        }
    }

    private fun scheduleComputerMoveIfNeeded() {
        val current = _state.value
        if (!current.isComputerTurn) return
        computerJob = viewModelScope.launch {
            delay(thinkTimeMs)
            when (current.variant) {
                GameVariant.CLASSIC_3X3 -> {
                    val move = withContext(computerDispatcher) { chooseComputerMove(current.match) }
                    dispatch(Intent.ComputerPlayed(move))
                }
                GameVariant.GRID_4X4, GameVariant.GRID_5X5 -> {
                    val board = current.gridBoard ?: return@launch
                    val ai = HeuristicGridAI(current.difficulty)
                    val move = withContext(computerDispatcher) { ai.chooseMove(board, Mark.O) }
                    dispatch(Intent.PlaceVariantMove(GameMove.Grid(move)))
                }
                GameVariant.ULTIMATE -> {
                    val board = current.ultimateBoard ?: return@launch
                    val ai = UltimateAI(current.difficulty)
                    val move = withContext(computerDispatcher) { ai.chooseMove(board, Mark.O) }
                    dispatch(Intent.PlaceVariantMove(move))
                }
            }
        }
    }

    companion object {
        const val DEFAULT_THINK_TIME_MS = 450L
    }
}
