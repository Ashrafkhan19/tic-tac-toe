package com.example.tictactoe.presentation

import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.player.ComputerPlayer
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.ChooseComputerMove
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = GameViewModel(
        reducer = GameReducer(PlayHumanMove(), ApplyComputerMove(), StartNextRound(), ChangeSettings(), ResetScores()),
        chooseComputerMove = ChooseComputerMove { ComputerPlayer { 4 } },
        computerDispatcher = dispatcher,
        thinkTimeMs = 450,
    )

    @Test
    fun computerRepliesAfterThinkTime() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(Intent.CellTapped(0))
        advanceTimeBy(449); runCurrent()
        assertNull(vm.state.value.game.board.cells[4])
        advanceTimeBy(2); runCurrent()
        assertEquals(Mark.O, vm.state.value.game.board.cells[4])
        assertEquals(false, vm.state.value.isComputerTurn)
    }

    @Test
    fun newInputCancelsPendingComputerMove() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(Intent.CellTapped(0))
        advanceTimeBy(100)
        vm.onIntent(Intent.ResetScores)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(9, vm.state.value.game.board.emptyCells.size)
    }
}
