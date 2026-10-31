package com.example.tictactoe.presentation

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound
import kotlin.test.Test
import kotlin.test.assertEquals

class GameReducerTest {
    private val reducer = GameReducer(PlayHumanMove(), ApplyComputerMove(), StartNextRound(), ChangeSettings(), ResetScores())
    private val twoPlayer = UiState(Match(settings = MatchSettings(mode = Mode.TwoPlayer)))
    private fun run(start: UiState, vararg intents: Intent) = intents.fold(start) { s, i -> reducer.reduce(s, i) }
    private fun taps(vararg cells: Int) = cells.map { Intent.CellTapped(it) }.toTypedArray<Intent>()

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
        val afterHuman = run(UiState(), Intent.CellTapped(0))
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
}
