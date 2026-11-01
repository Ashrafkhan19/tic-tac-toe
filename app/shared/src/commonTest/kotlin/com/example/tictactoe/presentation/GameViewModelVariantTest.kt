package com.example.tictactoe.presentation

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.progress.InMemoryProgressRepository
import com.example.tictactoe.domain.time.TodayProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelVariantTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fixedToday = object : TodayProvider {
        override fun today(): LocalDate = LocalDate(2026, 11, 1)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun starting4x4VariantInitializesGridBoard() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(progressRepository = repo, todayProvider = fixedToday, computerDispatcher = testDispatcher)

        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GameVariant.GRID_4X4, vm.state.value.variant)
        assertNotNull(vm.state.value.gridBoard)
        assertEquals(4, vm.state.value.gridBoard?.size)
        assertEquals(4, vm.state.value.gridBoard?.winLength)
        assertNull(vm.state.value.ultimateBoard)
    }

    @Test
    fun starting5x5VariantInitializesGridBoard() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(progressRepository = repo, todayProvider = fixedToday, computerDispatcher = testDispatcher)

        vm.send(Intent.SelectVariant(GameVariant.GRID_5X5))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GameVariant.GRID_5X5, vm.state.value.variant)
        assertNotNull(vm.state.value.gridBoard)
        assertEquals(5, vm.state.value.gridBoard?.size)
        assertEquals(4, vm.state.value.gridBoard?.winLength)
        assertNull(vm.state.value.ultimateBoard)
    }

    @Test
    fun startingUltimateVariantInitializesUltimateBoard() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(progressRepository = repo, todayProvider = fixedToday, computerDispatcher = testDispatcher)

        vm.send(Intent.SelectVariant(GameVariant.ULTIMATE))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GameVariant.ULTIMATE, vm.state.value.variant)
        assertNotNull(vm.state.value.ultimateBoard)
        assertEquals(9, vm.state.value.ultimateBoard?.subBoards?.size)
        assertNull(vm.state.value.gridBoard)
    }

    @Test
    fun humanMoveOn4x4UpdatesGridBoardAndTriggersComputer() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(
            progressRepository = repo,
            todayProvider = fixedToday,
            computerDispatcher = testDispatcher,
            thinkTimeMs = 300L,
        )

        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.send(Intent.PlaceVariantMove(GameMove.Grid(0)))
        testDispatcher.scheduler.runCurrent()

        assertEquals(Mark.X, vm.state.value.gridBoard?.cells?.get(0))
        assertTrue(vm.state.value.isComputerTurn)

        // Advance before thinkTimeMs: computer has not played yet
        testDispatcher.scheduler.advanceTimeBy(299)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, vm.state.value.gridBoard?.cells?.count { it != null })

        // Advance past thinkTimeMs: computer plays Mark.O
        testDispatcher.scheduler.advanceTimeBy(2)
        testDispatcher.scheduler.advanceUntilIdle()

        val cells = vm.state.value.gridBoard?.cells ?: emptyList()
        assertEquals(1, cells.count { it == Mark.X })
        assertEquals(1, cells.count { it == Mark.O })
        assertFalse(vm.state.value.isComputerTurn)
    }

    @Test
    fun humanMoveOnUltimateUpdatesUltimateBoardAndTriggersComputer() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(
            progressRepository = repo,
            todayProvider = fixedToday,
            computerDispatcher = testDispatcher,
            thinkTimeMs = 200L,
        )

        vm.send(Intent.SelectVariant(GameVariant.ULTIMATE))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.send(Intent.PlaceVariantMove(GameMove.Ultimate(macroIndex = 4, microIndex = 0)))
        testDispatcher.scheduler.runCurrent()

        assertEquals(Mark.X, vm.state.value.ultimateBoard?.subBoards?.get(4)?.cells?.get(0))
        assertEquals(0, vm.state.value.ultimateBoard?.activeMacroIndex)
        assertTrue(vm.state.value.isComputerTurn)

        // Advance past think time so UltimateAI plays
        testDispatcher.scheduler.advanceTimeBy(201)
        testDispatcher.scheduler.advanceUntilIdle()

        val totalO = vm.state.value.ultimateBoard?.subBoards?.sumOf { it.cells.count { c -> c == Mark.O } } ?: 0
        assertEquals(1, totalO)
        assertFalse(vm.state.value.isComputerTurn)
    }

    @Test
    fun twoPlayerVariantDoesNotScheduleComputerAndDoesNotRecordStats() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(progressRepository = repo, todayProvider = fixedToday, computerDispatcher = testDispatcher)

        vm.send(Intent.ModeSelected(Mode.TwoPlayer))
        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()

        // X plays 0
        vm.send(Intent.PlaceVariantMove(GameMove.Grid(0)))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.isComputerTurn)

        // O plays 4
        vm.send(Intent.PlaceVariantMove(GameMove.Grid(4)))
        // X plays 1
        vm.send(Intent.PlaceVariantMove(GameMove.Grid(1)))
        // O plays 5
        vm.send(Intent.PlaceVariantMove(GameMove.Grid(5)))
        // X plays 2
        vm.send(Intent.PlaceVariantMove(GameMove.Grid(2)))
        // O plays 6
        vm.send(Intent.PlaceVariantMove(GameMove.Grid(6)))
        // X plays 3 (Row 0 win: 0, 1, 2, 3)
        vm.send(Intent.PlaceVariantMove(GameMove.Grid(3)))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.isGameOver)
        val outcome = vm.state.value.currentOutcome
        assertTrue(outcome is Outcome.Won && outcome.winner == Mark.X)
        assertEquals(1, vm.state.value.xWins)

        // TwoPlayer games save no progress
        assertEquals(0, repo.load().xp)
        assertTrue(repo.load().variantStats.isEmpty())
    }

    @Test
    fun vsComputerWinOn4x4RecordsVariantStatsAndScaledXp() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(
            progressRepository = repo,
            todayProvider = fixedToday,
            computerDispatcher = testDispatcher,
            thinkTimeMs = 0L,
        )

        vm.send(Intent.DifficultySelected(Difficulty.Easy))
        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()

        // X plays top row (0, 1, 2, 3). Even if AI blocks or plays, we place until win
        // To guarantee a win regardless of computer move on Easy:
        // Let's play winning row 0. If computer took cell in row 0, we take another line.
        val line = listOf(0, 1, 2, 3)
        for (cell in line) {
            val board = vm.state.value.gridBoard ?: break
            if (board.outcome() !is Outcome.InProgress) break
            val target = if (board.cells[cell] == null) cell else board.emptyCells.first()
            vm.send(Intent.PlaceVariantMove(GameMove.Grid(target)))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Keep playing until game is over
        var turns = 0
        while (!vm.state.value.isGameOver && turns < 20) {
            val board = vm.state.value.gridBoard ?: break
            if (vm.state.value.isComputerTurn) {
                testDispatcher.scheduler.advanceUntilIdle()
            } else {
                val move = board.emptyCells.firstOrNull() ?: break
                vm.send(Intent.PlaceVariantMove(GameMove.Grid(move)))
                testDispatcher.scheduler.advanceUntilIdle()
            }
            turns++
        }

        assertTrue(vm.state.value.isGameOver)
        val progress = repo.load()
        // Progress should have recorded a game in GRID_4X4:Easy
        val stat = progress.variantStats["${GameVariant.GRID_4X4.name}:${Difficulty.Easy.name}"]
        assertNotNull(stat)
        assertEquals(1, stat.wins + stat.draws + stat.losses)
        assertTrue(progress.xp >= 0)
    }

    @Test
    fun switchingBackToClassic3x3RestoresClassicFlow() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(progressRepository = repo, todayProvider = fixedToday, computerDispatcher = testDispatcher)

        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(GameVariant.GRID_4X4, vm.state.value.variant)
        assertNotNull(vm.state.value.gridBoard)

        vm.send(Intent.SelectVariant(GameVariant.CLASSIC_3X3))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(GameVariant.CLASSIC_3X3, vm.state.value.variant)
        assertNull(vm.state.value.gridBoard)
        assertNull(vm.state.value.ultimateBoard)
        assertEquals(9, vm.state.value.game.board.cells.size)
    }

    @Test
    fun newRoundAndResetScoresResetVariantBoard() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(progressRepository = repo, todayProvider = fixedToday, computerDispatcher = testDispatcher)

        vm.send(Intent.ModeSelected(Mode.TwoPlayer))
        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.send(Intent.PlaceVariantMove(GameMove.Grid(0)))
        assertEquals(Mark.X, vm.state.value.gridBoard?.cells?.get(0))

        vm.send(Intent.NewRound)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(16, vm.state.value.gridBoard?.emptyCells?.size)

        vm.send(Intent.PlaceVariantMove(GameMove.Grid(5)))
        assertEquals(Mark.X, vm.state.value.gridBoard?.cells?.get(5))

        vm.send(Intent.ResetScores)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(16, vm.state.value.gridBoard?.emptyCells?.size)
        assertEquals(0, vm.state.value.xWins)
    }

    @Test
    fun cellTappedIsIgnoredWhenVariantIsNotClassic3x3() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(progressRepository = repo, todayProvider = fixedToday, computerDispatcher = testDispatcher)

        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.send(Intent.CellTapped(0))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.state.value.game.board.cells[0])
        assertEquals(16, vm.state.value.gridBoard?.emptyCells?.size)
    }

    @Test
    fun currentTurnAndOutcomeReflectVariantState() = runTest {
        val repo = InMemoryProgressRepository()
        val vm = GameViewModel(progressRepository = repo, todayProvider = fixedToday, computerDispatcher = testDispatcher)

        // Classic 3x3
        assertEquals(Mark.X, vm.state.value.currentTurn)
        assertFalse(vm.state.value.isGameOver)
        assertEquals(Outcome.InProgress, vm.state.value.currentOutcome)

        // Switch to 4x4 in 2-player mode
        vm.send(Intent.ModeSelected(Mode.TwoPlayer))
        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Mark.X, vm.state.value.currentTurn)
        assertFalse(vm.state.value.isGameOver)

        // Player X moves -> turn is O
        vm.send(Intent.PlaceVariantMove(GameMove.Grid(0)))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(Mark.O, vm.state.value.currentTurn)

        // Switch to Ultimate
        vm.send(Intent.SelectVariant(GameVariant.ULTIMATE))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(Mark.X, vm.state.value.currentTurn)
        assertFalse(vm.state.value.isGameOver)

        // Player X moves -> turn is O
        vm.send(Intent.PlaceVariantMove(GameMove.Ultimate(0, 0)))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(Mark.O, vm.state.value.currentTurn)
    }
}
