package com.example.tictactoe.presentation

import com.example.tictactoe.domain.challenge.DailyChallenge
import com.example.tictactoe.domain.challenge.DailyChallengeGenerator
import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.player.ComputerPlayer
import com.example.tictactoe.domain.progress.DifficultyStats
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    /** A Monday: the challenge is a forced win in one move. */
    private val dayD = LocalDate(2026, 11, 2)
    private var today = dayD
    private val repository = InMemoryProgressRepository()
    private val generator = DailyChallengeGenerator()

    private fun viewModel(
        computer: ComputerPlayer = ComputerPlayer { 4 },
        thinkTimeMs: Long = 450,
        repository: ProgressRepository = this.repository,
        challenges: DailyChallengeGenerator = generator,
    ) = GameViewModel(
        reducer = GameReducer(PlayHumanMove(), ApplyComputerMove(), StartNextRound(), ChangeSettings(), ResetScores()),
        chooseComputerMove = ChooseComputerMove { computer },
        repository = repository,
        recordGameResult = RecordGameResult(),
        completeDailyChallenge = CompleteDailyChallenge(),
        today = TodayProvider { today },
        challenges = challenges,
        computerDispatcher = dispatcher,
        thinkTimeMs = thinkTimeMs,
    )

    /** Plays the first free cell from a fixed preference list: O loses to X taking the top row. */
    private val passiveComputer = ComputerPlayer { game -> listOf(3, 4, 5, 6, 7, 8, 0, 1, 2).first { game.board.cells[it] == null } }

    /** Wins when it can, otherwise takes the first free cell. */
    private val greedyComputer = ComputerPlayer { game -> winningCell(game, Mark.O) ?: game.board.emptyCells.first() }

    private fun winningCell(game: GameState, mark: Mark): Int? = game.board.emptyCells.firstOrNull {
        (game.board.place(it, mark).outcome() as? Outcome.Won)?.winner == mark
    }

    private fun TestScope.tap(vm: GameViewModel, cell: Int) {
        vm.onIntent(Intent.CellTapped(cell))
        advanceUntilIdle()
    }

    private fun TestScope.winChallenge(vm: GameViewModel) {
        while (!vm.state.value.game.isOver) {
            val game = vm.state.value.game
            tap(vm, winningCell(game, Mark.X) ?: game.board.emptyCells.first())
        }
        assertEquals(Mark.X, (vm.state.value.game.outcome as Outcome.Won).winner)
    }

    /** Plays X moves, found by simulating [greedyComputer], that end the round as a draw or an O win. */
    private fun TestScope.failChallenge(vm: GameViewModel) {
        val line = assertNotNull(nonWinningLine(vm.state.value.game), "no losing line for this position")
        line.forEach { tap(vm, it) }
        assertTrue(vm.state.value.game.isOver)
        assertNotEquals(Mark.X, (vm.state.value.game.outcome as? Outcome.Won)?.winner)
    }

    private fun nonWinningLine(game: GameState): List<Int>? {
        for (cell in game.board.emptyCells) {
            val afterX = game.play(cell)
            val outcome = afterX.outcome
            if (outcome is Outcome.Won) continue
            if (outcome == Outcome.Draw) return listOf(cell)
            val afterO = afterX.play(greedyComputer.chooseMove(afterX))
            if (afterO.isOver) return listOf(cell)
            nonWinningLine(afterO)?.let { return listOf(cell) + it }
        }
        return null
    }

    // --- original behaviour ---

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

    // --- startup ---

    @Test
    fun firstStateIsPlayableWhileTheChallengeLoadsInTheBackground() = runTest(dispatcher) {
        val vm = viewModel(thinkTimeMs = 0)
        assertEquals(dayD, vm.state.value.today)
        assertNull(vm.state.value.todayChallenge)
        vm.onIntent(Intent.CellTapped(0))
        assertEquals(Mark.X, vm.state.value.game.board.cells[0])
        advanceUntilIdle()
        assertEquals(generator.forDate(dayD), vm.state.value.todayChallenge)
    }

    @Test
    fun exposesProgressAlreadyInTheRepository() = runTest(dispatcher) {
        val stored = PlayerProgress(xp = 120, stats = mapOf(Difficulty.Easy to DifficultyStats(wins = 3)))
        val vm = viewModel(repository = InMemoryProgressRepository(stored))
        assertEquals(stored, vm.state.value.progress)
    }

    // --- free play ---

    @Test
    fun finishingAFreeEasyWinSavesXpAndAWin() = runTest(dispatcher) {
        val vm = viewModel(computer = passiveComputer, thinkTimeMs = 0)
        vm.onIntent(Intent.DifficultySelected(Difficulty.Easy))
        listOf(0, 1, 2).forEach { tap(vm, it) }
        assertEquals(Mark.X, (vm.state.value.game.outcome as Outcome.Won).winner)
        val expected = PlayerProgress(xp = 10, stats = mapOf(Difficulty.Easy to DifficultyStats(wins = 1)))
        assertEquals(expected, repository.load())
        assertEquals(expected, vm.state.value.progress)
    }

    @Test
    fun twoPlayerGamesSaveNothing() = runTest(dispatcher) {
        val vm = viewModel(thinkTimeMs = 0)
        vm.onIntent(Intent.ModeSelected(Mode.TwoPlayer))
        listOf(0, 3, 1, 4, 2).forEach { tap(vm, it) }
        assertTrue(vm.state.value.game.isOver)
        assertEquals(PlayerProgress(), repository.load())
        assertEquals(PlayerProgress(), vm.state.value.progress)
    }

    @Test
    fun aFinishedRoundIsRecordedOnceEvenIfTheSameStateIsDispatchedAgain() = runTest(dispatcher) {
        val saves = mutableListOf<PlayerProgress>()
        val counting = object : ProgressRepository by repository {
            override fun save(progress: PlayerProgress) {
                saves += progress
                repository.save(progress)
            }
        }
        val vm = viewModel(computer = passiveComputer, thinkTimeMs = 0, repository = counting)
        vm.onIntent(Intent.DifficultySelected(Difficulty.Easy))
        listOf(0, 1, 2).forEach { tap(vm, it) }
        val finished = vm.state.value
        tap(vm, 8)
        vm.onIntent(Intent.ComputerPlayed(8)); advanceUntilIdle()
        vm.onIntent(Intent.CellTapped(2)); advanceUntilIdle()
        assertEquals(finished, vm.state.value)
        assertEquals(1, saves.size)
        assertEquals(10, repository.load().xp)
    }

    @Test
    fun resetScoresKeepsProgress() = runTest(dispatcher) {
        val vm = viewModel(computer = passiveComputer, thinkTimeMs = 0)
        vm.onIntent(Intent.DifficultySelected(Difficulty.Easy))
        listOf(0, 1, 2).forEach { tap(vm, it) }
        vm.onIntent(Intent.ResetScores)
        assertEquals(0, vm.state.value.xWins)
        assertEquals(10, vm.state.value.progress.xp)
        assertEquals(10, repository.load().xp)
    }

    // --- daily challenge ---

    @Test
    fun startingTheChallengeSetsInChallengeAndThePresetBoard() = runTest(dispatcher) {
        val vm = viewModel(thinkTimeMs = 0)
        advanceUntilIdle()
        vm.onIntent(Intent.StartDailyChallenge)
        assertTrue(vm.state.value.inChallenge)
        assertEquals(generator.forDate(dayD).game, vm.state.value.game)
        assertEquals(Difficulty.Unbeatable, vm.state.value.difficulty)
    }

    @Test
    fun winningTheChallengeAdvancesTheStreakAndAddsTheBonusOnce() = runTest(dispatcher) {
        val vm = viewModel(computer = greedyComputer, thinkTimeMs = 0)
        advanceUntilIdle()
        vm.onIntent(Intent.StartDailyChallenge)
        winChallenge(vm)
        val progress = vm.state.value.progress
        assertEquals(1, progress.streak.current)
        assertEquals(dayD, progress.streak.lastCompleted)
        assertEquals(50, progress.xp)
        assertTrue(progress.stats.isEmpty(), "challenge games do not touch per-difficulty stats")
        assertEquals(progress, repository.load())

        vm.onIntent(Intent.NewRound)
        assertEquals(generator.forDate(dayD).game, vm.state.value.game)
        winChallenge(vm)
        assertEquals(progress, vm.state.value.progress)
        assertEquals(progress, repository.load())
    }

    @Test
    fun losingTheChallengeChangesNothingAndNewRoundRestoresThePosition() = runTest(dispatcher) {
        val vm = viewModel(computer = greedyComputer, thinkTimeMs = 0)
        advanceUntilIdle()
        vm.onIntent(Intent.StartDailyChallenge)
        failChallenge(vm)
        assertEquals(PlayerProgress(), vm.state.value.progress)
        assertEquals(PlayerProgress(), repository.load())
        vm.onIntent(Intent.NewRound)
        assertEquals(generator.forDate(dayD).game, vm.state.value.game)
        assertTrue(vm.state.value.inChallenge)
    }

    @Test
    fun aChallengeStartedOnDayDAndFinishedAfterMidnightIsCreditedToD() = runTest(dispatcher) {
        val vm = viewModel(computer = greedyComputer, thinkTimeMs = 0)
        advanceUntilIdle()
        vm.onIntent(Intent.StartDailyChallenge)
        today = dayD.plus(DatePeriod(days = 1))
        vm.onIntent(Intent.RefreshDate)
        advanceUntilIdle()
        assertEquals(dayD, vm.state.value.todayChallenge?.date, "not regenerated while a challenge round is in progress")
        assertEquals(generator.forDate(dayD).game, vm.state.value.game)
        winChallenge(vm)
        assertEquals(dayD, vm.state.value.progress.streak.lastCompleted)
        assertEquals(dayD, repository.load().streak.lastCompleted)
    }

    @Test
    fun aChallengeRetriedAfterMidnightIsStillCreditedToD() = runTest(dispatcher) {
        val vm = viewModel(computer = greedyComputer, thinkTimeMs = 0)
        advanceUntilIdle()
        vm.onIntent(Intent.StartDailyChallenge)
        failChallenge(vm)
        today = dayD.plus(DatePeriod(days = 1))
        vm.onIntent(Intent.RefreshDate)
        advanceUntilIdle()
        vm.onIntent(Intent.NewRound)
        assertEquals(generator.forDate(dayD).game, vm.state.value.game)
        winChallenge(vm)
        assertEquals(dayD, vm.state.value.progress.streak.lastCompleted)
    }

    @Test
    fun refreshDateInFreePlayRollsOverToTheNextChallenge() = runTest(dispatcher) {
        val vm = viewModel(thinkTimeMs = 0)
        advanceUntilIdle()
        val dayAfter = dayD.plus(DatePeriod(days = 1))
        today = dayAfter
        vm.onIntent(Intent.RefreshDate)
        assertEquals(dayAfter, vm.state.value.today)
        assertNull(vm.state.value.todayChallenge, "a stale challenge is never offered for the new day")
        advanceUntilIdle()
        assertEquals(generator.forDate(dayAfter), vm.state.value.todayChallenge)
    }

    @Test
    fun refreshDateOnTheSameDayKeepsTheChallenge() = runTest(dispatcher) {
        val vm = viewModel(thinkTimeMs = 0)
        advanceUntilIdle()
        val loaded = vm.state.value
        vm.onIntent(Intent.RefreshDate)
        assertEquals(loaded, vm.state.value)
    }

    @Test
    fun leaveDailyChallengeRestoresFreePlayAndCatchesUpWithTheDate() = runTest(dispatcher) {
        val vm = viewModel(computer = passiveComputer, thinkTimeMs = 0)
        advanceUntilIdle()
        vm.onIntent(Intent.DifficultySelected(Difficulty.Easy))
        listOf(0, 1, 2).forEach { tap(vm, it) }
        vm.onIntent(Intent.NewRound)
        val freePlay = vm.state.value.match
        assertEquals(1, freePlay.scoreboard.xWins)
        vm.onIntent(Intent.StartDailyChallenge)
        tap(vm, vm.state.value.game.board.emptyCells.first())
        today = dayD.plus(DatePeriod(days = 1))
        vm.onIntent(Intent.LeaveDailyChallenge)
        assertFalse(vm.state.value.inChallenge)
        assertEquals(freePlay, vm.state.value.match)
        assertEquals(Difficulty.Easy, vm.state.value.difficulty)
        advanceUntilIdle()
        assertEquals(today, vm.state.value.today)
        assertNotNull(vm.state.value.todayChallenge)
        assertEquals(today, vm.state.value.todayChallenge?.date)
    }

    @Test
    fun playTappedAfterMidnightInTheForegroundStartsTheNewDaysPuzzle() = runTest(dispatcher) {
        val vm = viewModel(thinkTimeMs = 0)
        advanceUntilIdle()
        val dayAfter = dayD.plus(DatePeriod(days = 1))
        today = dayAfter
        vm.onIntent(Intent.StartDailyChallenge)
        advanceUntilIdle()
        assertEquals(dayAfter, vm.state.value.today)
        assertEquals(dayAfter, vm.state.value.todayChallenge?.date)
        assertTrue(vm.state.value.inChallenge)
        assertEquals(generator.forDate(dayAfter).game, vm.state.value.game)
    }

    @Test
    fun aFailingGeneratorLeavesNoChallengeAndAPlayableBoard() = runTest(dispatcher) {
        val failing = object : DailyChallengeGenerator() {
            override fun forDate(date: LocalDate): DailyChallenge = error("boom")
        }
        val vm = viewModel(thinkTimeMs = 0, challenges = failing)
        advanceUntilIdle()
        assertNull(vm.state.value.todayChallenge)
        vm.onIntent(Intent.StartDailyChallenge)
        assertFalse(vm.state.value.inChallenge)
        tap(vm, 0)
        assertEquals(Mark.X, vm.state.value.game.board.cells[0])
    }
}
