package com.example.tictactoe.presentation

import com.example.tictactoe.domain.challenge.DailyChallenge
import com.example.tictactoe.domain.model.Board
import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.StreakState
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameReducerTest {
    private val reducer = GameReducer(PlayHumanMove(), ApplyComputerMove(), StartNextRound(), ChangeSettings(), ResetScores())
    private val day = LocalDate(2026, 11, 2)
    private val twoPlayer = UiState(Match(settings = MatchSettings(mode = Mode.TwoPlayer)), today = day)
    private fun run(start: UiState, vararg intents: Intent) = intents.fold(start) { s, i -> reducer.reduce(s, i) }
    private fun taps(vararg cells: Int) = cells.map { Intent.CellTapped(it) }.toTypedArray<Intent>()

    /** X to move at 0, 1 to complete the top row after 2. */
    private val challenge = DailyChallenge(
        date = day,
        game = GameState(Board(listOf(Mark.X, Mark.X, null, Mark.O, Mark.O, null, null, null, null)), Mark.X),
        movesToWin = 1,
    )
    private val withChallenge = UiState(today = day, todayChallenge = challenge)

    @Test
    fun tapPlacesMarkAndPassesTurn() {
        val s = run(twoPlayer, Intent.CellTapped(0))
        assertEquals(Mark.X, s.game.board.cells[0])
        assertEquals(Mark.O, s.game.turn)
    }

    @Test
    fun winUpdatesScoreboardExposedToUi() {
        assertEquals(1, run(twoPlayer, *taps(0, 3, 1, 4, 2)).xWins)
    }

    @Test
    fun computerReplyIsAppliedOnlyOnItsTurn() {
        val afterHuman = run(UiState(today = day), Intent.CellTapped(0))
        assertEquals(true, afterHuman.isComputerTurn)
        val afterComputer = reducer.reduce(afterHuman, Intent.ComputerPlayed(4))
        assertEquals(Mark.O, afterComputer.game.board.cells[4])
        assertEquals(afterComputer, reducer.reduce(afterComputer, Intent.ComputerPlayed(5)))
    }

    @Test
    fun settingsIntentsResetScoresAndKeepTheOtherSetting() {
        val won = run(twoPlayer, *taps(0, 3, 1, 4, 2))
        val easy = reducer.reduce(won, Intent.DifficultySelected(Difficulty.Easy))
        assertEquals(0, easy.xWins)
        assertEquals(Mode.TwoPlayer, easy.mode)
        assertEquals(Difficulty.Easy, reducer.reduce(easy, Intent.ModeSelected(Mode.VsComputer)).difficulty)
    }

    @Test
    fun newRoundKeepsScoresAndResetScoresClearsThem() {
        val won = run(twoPlayer, *taps(0, 3, 1, 4, 2))
        assertEquals(1, reducer.reduce(won, Intent.NewRound).xWins)
        assertEquals(0, reducer.reduce(won, Intent.ResetScores).xWins)
    }

    @Test
    fun reducerKeepsNonMatchFieldsWhenPlaying() {
        val start = twoPlayer.copy(progress = PlayerProgress(xp = 77), todayChallenge = challenge)
        val s = run(start, *taps(0, 3, 1, 4, 2), Intent.NewRound, Intent.ResetScores)
        assertEquals(77, s.progress.xp)
        assertEquals(day, s.today)
        assertEquals(challenge, s.todayChallenge)
    }

    @Test
    fun startDailyChallengeIsIgnoredUntilTheChallengeIsLoaded() {
        val loading = UiState(today = day)
        assertEquals(loading, reducer.reduce(loading, Intent.StartDailyChallenge))
    }

    @Test
    fun startDailyChallengeLoadsThePresetBoardAgainstUnbeatable() {
        val s = run(withChallenge, Intent.DifficultySelected(Difficulty.Easy), Intent.StartDailyChallenge)
        assertTrue(s.inChallenge)
        assertEquals(MatchSettings(Mode.VsComputer, Difficulty.Unbeatable), s.match.settings)
        assertEquals(challenge.game, s.game)
    }

    @Test
    fun settingsAreIgnoredAndNewRoundRestartsThePositionDuringAChallenge() {
        val started = run(withChallenge, Intent.StartDailyChallenge, Intent.CellTapped(5))
        val s = run(started, Intent.ModeSelected(Mode.TwoPlayer), Intent.DifficultySelected(Difficulty.Easy))
        assertEquals(started, s)
        val restarted = reducer.reduce(s, Intent.NewRound)
        assertEquals(challenge.game, restarted.game)
        assertTrue(restarted.inChallenge)
        val reset = reducer.reduce(s, Intent.ResetScores)
        assertEquals(challenge.game, reset.game)
        assertTrue(reset.inChallenge)
    }

    @Test
    fun leaveDailyChallengeRestoresThePreChallengeMatch() {
        val start = withChallenge.copy(match = Match(MatchSettings(Mode.TwoPlayer, Difficulty.Easy)))
        val freePlay = run(start, *taps(0, 3, 1, 4, 2), Intent.NewRound, Intent.CellTapped(8))
        assertEquals(1, freePlay.xWins)
        val s = run(freePlay, Intent.StartDailyChallenge, Intent.CellTapped(5), Intent.NewRound, Intent.LeaveDailyChallenge)
        assertFalse(s.inChallenge)
        assertEquals(freePlay.match, s.match)
        assertEquals(challenge, s.todayChallenge)
    }

    @Test
    fun startingTheChallengeAgainKeepsTheOriginalFreePlayMatch() {
        val freePlay = run(withChallenge, Intent.DifficultySelected(Difficulty.Medium), Intent.CellTapped(8))
        val s = run(freePlay, Intent.StartDailyChallenge, Intent.StartDailyChallenge, Intent.LeaveDailyChallenge)
        assertEquals(freePlay.match, s.match)
    }

    @Test
    fun challengeIsDoneWhenCompletedOnOrAfterItsDate() {
        fun completedOn(date: LocalDate?) = withChallenge.copy(progress = PlayerProgress(streak = StreakState(current = 1, lastCompleted = date)))
        assertFalse(completedOn(null).dailyChallengeDone)
        assertFalse(completedOn(LocalDate(2026, 11, 1)).dailyChallengeDone)
        assertTrue(completedOn(day).dailyChallengeDone)
        assertTrue(completedOn(LocalDate(2026, 11, 3)).dailyChallengeDone, "clock moved backwards after completing a later day")
        assertFalse(UiState(today = day).dailyChallengeDone, "nothing is done while loading")
    }
}
