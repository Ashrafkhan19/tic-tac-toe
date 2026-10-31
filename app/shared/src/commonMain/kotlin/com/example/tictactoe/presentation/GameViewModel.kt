package com.example.tictactoe.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tictactoe.domain.usecase.ChooseComputerMove
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val reducer: GameReducer,
    private val chooseComputerMove: ChooseComputerMove,
    private val computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val thinkTimeMs: Long = DEFAULT_THINK_TIME_MS,
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var computerJob: Job? = null

    fun onIntent(intent: Intent) {
        // Any new input invalidates a pending computer move.
        computerJob?.cancel()
        dispatch(intent)
    }

    private fun dispatch(intent: Intent) {
        _state.update { reducer.reduce(it, intent) }
        scheduleComputerMoveIfNeeded()
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
