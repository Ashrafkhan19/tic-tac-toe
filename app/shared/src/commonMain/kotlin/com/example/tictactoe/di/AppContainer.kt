package com.example.tictactoe.di

import com.example.tictactoe.domain.player.ComputerPlayerProvider
import com.example.tictactoe.domain.player.DefaultComputerPlayerProvider
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.ChooseComputerMove
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound
import com.example.tictactoe.presentation.GameReducer
import com.example.tictactoe.presentation.GameViewModel

/**
 * Composition root: the only place that knows which concrete classes implement the domain
 * interfaces. Swap [computerPlayers] to change how the computer plays.
 */
class AppContainer(
    private val computerPlayers: ComputerPlayerProvider = DefaultComputerPlayerProvider(),
) {
    fun createGameViewModel(): GameViewModel = GameViewModel(
        reducer = GameReducer(
            playHumanMove = PlayHumanMove(),
            applyComputerMove = ApplyComputerMove(),
            startNextRound = StartNextRound(),
            changeSettings = ChangeSettings(),
            resetScores = ResetScores(),
        ),
        chooseComputerMove = ChooseComputerMove(computerPlayers),
    )
}
